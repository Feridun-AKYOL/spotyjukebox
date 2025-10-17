package org.bithub.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PlayedSongRepository playedSongRepository;

    // 🕐 1️⃣ Otomatik temizlik — her 5 dakikada bir 1 saatten eski oyları sil
    @Scheduled(fixedRate = 60000) // 300,000 ms = 5 dk ...->simdilik 1 dk ayarli
    @Transactional
    public void cleanupOldVotesScheduled() {
        cleanupOldVotes();
    }

    // her 5 dakikada 1 saatten eski oyları temizle
    @Transactional
    public void cleanupOldVotes() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        voteRepository.deleteOldVotes(threshold);
    }

    // Oy ekle
    @Transactional
    public Vote addVote(String ownerId, String trackId, String clientId) {
        cleanupOldVotes();

        // 🔹 Aynı kullanıcı aynı şarkıya zaten oy verdiyse, yeni oy verme
        List<Vote> existingVotes = voteRepository.findByOwnerIdAndTrackId(ownerId, trackId);
        boolean alreadyVoted = existingVotes.stream()
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
        return voteRepository.save(vote);
    }

    // Şarkı çaldığında oyları sıfırla
    @Transactional
    public void resetVotesForPlayedTrack(String ownerId, String trackId) {
        System.out.println("Resetting votes for track: " + trackId);

        voteRepository.deleteVotesForTrack(ownerId, trackId);

        // Silme sonrası kontrol
        long remaining = voteRepository.findByOwnerIdAndTrackId(ownerId, trackId).size();
        if (remaining == 0) {
            System.out.println("✅ Votes successfully reset for " + trackId);
        } else {
            System.err.println("⚠️ Votes not deleted correctly for " + trackId + " (" + remaining + " left)");
        }

        // Played listesine ekle
        playedSongRepository.save(PlayedSong.builder()
                .ownerId(ownerId)
                .trackId(trackId)
                .playedAt(LocalDateTime.now())
                .build());
    }

    // Geçerli oyları (1 saat içindekiler)
    public Map<String, Long> getActiveVotes(String ownerId) {
        cleanupOldVotes();
        Map<String, Long> result = new HashMap<>();

        for (Object[] row : voteRepository.findRecentVoteCounts(ownerId, LocalDateTime.now().minusHours(1))) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    // Son 3 çalınan şarkıyı getir
    public List<String> getCooldownTracks(String ownerId) {
        return playedSongRepository.findLast3Songs(ownerId);
    }

    public List<TrackVote> getRankedTracks(String ownerId) {
        Map<String, Long> votes = getActiveVotes(ownerId);
        return votes.entrySet().stream()
                .map(e -> new TrackVote(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.votes(), a.votes()))
                .toList();
    }

}
