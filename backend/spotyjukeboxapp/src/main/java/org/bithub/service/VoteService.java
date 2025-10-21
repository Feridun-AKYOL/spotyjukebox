package org.bithub.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bithub.model.PlayedSong;
import org.bithub.model.TrackVote;
import org.bithub.model.Vote;
import org.bithub.persistence.PlayedSongRepository;
import org.bithub.persistence.VoteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VoteService
 * ------------------------------------------------------------------------
 * Manages all vote-related logic for the Spotify Jukebox system.
 *
 * Responsibilities:
 *   • Adding votes while preventing duplicate votes per client
 *   • Cleaning up expired votes (older than 1 hour)
 *   • Resetting votes for songs that have finished playing
 *   • Tracking recently played songs (cooldown)
 *   • Returning ranked tracks based on current votes
 *
 * Backward Compatibility:
 *   All original public method names (addVote, resetVotesForPlayedTrack, etc.)
 *   have been preserved exactly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PlayedSongRepository playedSongRepository;

    // --------------------------------------------------------------------
    // 🧹 VOTE CLEANUP
    // --------------------------------------------------------------------

    /**
     * Scheduled cleanup task — removes votes older than 1 hour.
     * <p>
     * Currently runs every 1 minute for testing purposes.
     * In production, adjust {@code fixedRate} to 300000 (5 minutes).
     * </p>
     */
    @Scheduled(fixedRate = 60_000) // 1 minute (test) → change to 5 min in production
    @Transactional
    public void cleanupOldVotesScheduled() {
        cleanupOldVotes();
    }

    /**
     * Deletes all votes older than 1 hour from the repository.
     * Executed both manually and via scheduler.
     */
    @Transactional
    public void cleanupOldVotes() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        int deleted = voteRepository.deleteOldVotes(threshold);
        if (deleted > 0) {
            log.info("🧹 Cleaned {} expired votes (before {}).", deleted, threshold);
        }
    }


    // --------------------------------------------------------------------
    // 🗳️ ADD / MANAGE VOTES
    // --------------------------------------------------------------------

    /**
     * Adds a new vote for a track by a given client.
     * Prevents the same client from voting twice for the same song.
     *
     * @param ownerId  Spotify user/session ID of the Jukebox owner
     * @param trackId  Spotify track ID
     * @param clientId unique client identifier (browser/device)
     * @return persisted {@link Vote} object
     * @throws RuntimeException if this client already voted for the track
     */
    @Transactional
    public Vote addVote(String ownerId, String trackId, String clientId) {
        cleanupOldVotes();

        boolean alreadyVoted = voteRepository.findByOwnerIdAndTrackId(ownerId, trackId)
                .stream()
                .anyMatch(v -> v.getClientId().equals(clientId));

        if (alreadyVoted) {
            throw new RuntimeException("You have already voted for this song.");
        }

        Vote vote = Vote.builder()
                .ownerId(ownerId)
                .trackId(trackId)
                .clientId(clientId)
                .createdAt(LocalDateTime.now())
                .build();

        Vote saved = voteRepository.save(vote);
        log.info("🗳️ Added new vote → owner={} track={} client={}", ownerId, trackId, clientId);
        return saved;
    }


    // --------------------------------------------------------------------
    // 🎵 PLAYED TRACKS & RESET
    // --------------------------------------------------------------------

    /**
     * Resets (deletes) all votes for a track once it finishes playing,
     * and records the song in {@link PlayedSongRepository} for cooldown tracking.
     *
     * @param ownerId Spotify user/session ID
     * @param trackId Spotify track ID
     */
    @Transactional
    public void resetVotesForPlayedTrack(String ownerId, String trackId) {
        log.info("Resetting votes for track: {}", trackId);

        voteRepository.deleteVotesForTrack(ownerId, trackId);
        long remaining = voteRepository.findByOwnerIdAndTrackId(ownerId, trackId).size();

        if (remaining == 0) {
            log.info("✅ Votes successfully reset for {}", trackId);
        } else {
            log.warn("⚠️ Votes not fully deleted for {} ({} remaining)", trackId, remaining);
        }

        playedSongRepository.save(PlayedSong.builder()
                .ownerId(ownerId)
                .trackId(trackId)
                .playedAt(LocalDateTime.now())
                .build());
    }


    // --------------------------------------------------------------------
    // 📊 ACTIVE VOTES / RANKING
    // --------------------------------------------------------------------

    /**
     * Returns all active votes (within the last hour) as a map of track IDs and counts.
     *
     * @param ownerId Spotify user/session ID
     * @return map of trackId → voteCount
     */
    public Map<String, Long> getActiveVotes(String ownerId) {
        cleanupOldVotes();

        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        Map<String, Long> result = new HashMap<>();

        for (Object[] row : voteRepository.findRecentVoteCounts(ownerId, cutoff)) {
            result.put((String) row[0], (Long) row[1]);
        }

        return result;
    }

    /**
     * Retrieves the last 3 played tracks for cooldown management.
     * These tracks cannot be voted on again until they expire.
     *
     * @param ownerId Spotify user/session ID
     * @return list of last 3 track IDs
     */
    public List<String> getCooldownTracks(String ownerId) {
        return playedSongRepository.findLast3Songs(ownerId);
    }

    /**
     * Returns a ranked list of tracks with their vote counts,
     * sorted in descending order.
     *
     * @param ownerId Spotify user/session ID
     * @return sorted list of {@link TrackVote} objects
     */
    public List<TrackVote> getRankedTracks(String ownerId) {
        Map<String, Long> votes = getActiveVotes(ownerId);

        return votes.entrySet().stream()
                .map(e -> new TrackVote(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.votes(), a.votes()))
                .toList();
    }

    @Transactional
    public void addPlayedSong(String userId, String trackId) {
        try {
            // DB’de aynı şarkı son 3 içinde varsa tekrar ekleme
            List<String> recent = getCooldownTracks(userId);
            if (recent.contains(trackId)) return;

            playedSongRepository.save(
                    PlayedSong.builder()
                            .ownerId(userId)
                            .trackId(trackId)
                            .build()
            );

            if (recent.size() >= 3) {
                String oldestTrackId = recent.get(0); // ilk eklenen
                playedSongRepository.deleteByOwnerIdAndTrackId(userId, oldestTrackId);
                log.debug("🗑️ Removed oldest cooldown track {}", oldestTrackId);
            }

            log.debug("🎶 Added {} to cooldown for {}", trackId, userId);
        } catch (Exception e) {
            log.warn("⚠️ Failed to add played song {} for {}", trackId, userId, e);
        }
    }
}