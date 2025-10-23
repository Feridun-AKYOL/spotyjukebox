package org.bithub.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bithub.model.PlayedSong;
import org.bithub.model.TrackVote;
import org.bithub.model.Vote;
import org.bithub.persistence.PlayedSongRepository;
import org.bithub.persistence.VoteRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


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
    private final RedisTemplate<String, String> redisTemplate;

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

   /**
           * Belirli bir şarkının cooldown'dan çıkması için kaç şarkı daha çalması gerektiğini hesaplar
            *
            * @param ownerId Spotify kullanıcı ID'si
            * @param trackId Kontrol edilecek şarkının ID'si
            * @return Kalan şarkı sayısı (0 = cooldown'da değil, artık oylanabilir)
            */
            public int getCooldownRemaining(String ownerId, String trackId) {
        try {
            // Son 3 çalınan şarkıyı al (cooldown window'unuz)
            List<String> recentTracks = getRecentlyPlayedTrackIds(ownerId,3 );

            // Şarkı cooldown listesinde var mı?
            int position = recentTracks.indexOf(trackId);

            if (position == -1) {
                // Cooldown'da değil
                return 0;
            }

            // Cooldown'dan çıkması için kaç şarkı daha çalması gerekiyor

            int remaining = 3 - position;
            return Math.max(remaining, 0);

        } catch (Exception e) {
            log.warn("⚠️ Cooldown check failed for track {}: {}", trackId, e.getMessage());
            return 0; // Hata durumunda oylanabilir varsay
        }
    }

    /**
     * Son N adet çalınan şarkının ID'lerini döndürür (en yeniden en eskiye)
     * Redis'ten play history'yi çeker
     *
     * @param ownerId Spotify kullanıcı ID'si
     * @param limit Kaç şarkı getirilecek
     * @return Şarkı ID'leri listesi
     */
    private List<String> getRecentlyPlayedTrackIds(String ownerId, int limit) {
        String historyKey = "jukebox:history:" + ownerId;

        try {
            // Redis'te List olarak tutuyorsanız
            List<String> rawHistory = redisTemplate.opsForList()
                    .range(historyKey, 0, limit - 1);


            if (rawHistory == null || rawHistory.isEmpty()) {
                return Collections.emptyList();
            }

            return rawHistory.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Failed to fetch play history for {}", ownerId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Bir şarkı çalındığında history'ye ekler (en başa ekler - LPUSH mantığı)
     * Bu metod zaten mevcut olmalı, yoksa ekleyin
     *
     * @param ownerId Spotify kullanıcı ID'si
     * @param trackId Çalınan şarkının ID'si
     */
    public void addToPlayHistory(String ownerId, String trackId) {
        String historyKey = "jukebox:history:" + ownerId;

        try {
            // 🔹 Son eklenen şarkıyı al
            List<String> last = redisTemplate.opsForList().range(historyKey, 0, 0);

            // 🔸 Eğer aynıysa tekrar ekleme
            if (last != null && !last.isEmpty() && last.get(0).equals(trackId)) {
                log.debug("⏭️ Skipped duplicate track in history: {}", trackId);
                return;
            }

            // En başa ekle (en yeni şarkı index 0'da olacak)
            redisTemplate.opsForList().leftPush(historyKey, trackId);

            // Listeyi 10 şarkı ile sınırla (cooldown 5 ama biraz buffer)
            redisTemplate.opsForList().trim(historyKey, 0, 9);

            // 1 saat expire süresi
            redisTemplate.expire(historyKey, 1, TimeUnit.HOURS);

            log.info("✅ Added {} to play history for {}", trackId, ownerId);

        } catch (Exception e) {
            log.error("❌ Failed to add to play history", e);
        }
    }

    /**
     * Bir şarkı cooldown'da mı kontrol eder
     *
     * @param ownerId Spotify kullanıcı ID'si
     * @param trackId Kontrol edilecek şarkı
     * @return true = cooldown'da, false = oylanabilir
     */
    public boolean isInCooldown(String ownerId, String trackId) {
        return getCooldownRemaining(ownerId, trackId) > 0;
    }
}