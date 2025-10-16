package org.bithub.service;

import lombok.RequiredArgsConstructor;
import org.bithub.model.PlayedSong;
import org.bithub.model.Vote;
import org.bithub.persistence.PlayedSongRepository;
import org.bithub.persistence.VoteRepository;
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

    // 1 saatten eski oyları temizle
    public void cleanupOldVotes() {
        voteRepository.deleteOldVotes(LocalDateTime.now().minusHours(1));
    }

    // Oy ekle
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
                .build();
        return voteRepository.save(vote);
    }

    // Şarkı çaldığında oyları sıfırla
    public void resetVotesForPlayedTrack(String ownerId, String trackId) {
        voteRepository.deleteByTrackId(ownerId, trackId);

        // Played listesine ekle
        playedSongRepository.save(PlayedSong.builder()
                .ownerId(ownerId)
                .trackId(trackId)
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
}
