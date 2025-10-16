package org.bithub.controller;

import lombok.RequiredArgsConstructor;
import org.bithub.model.Vote;
import org.bithub.service.VoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jukebox")
@CrossOrigin(origins = "http://127.0.0.1:5173")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket için

    @PostMapping("/vote")
    public ResponseEntity<?> vote(@RequestBody Map<String, String> payload) {
        try {
            String ownerId = payload.get("ownerId");
            String trackId = payload.get("trackId");
            String clientId = payload.getOrDefault("clientId", UUID.randomUUID().toString());

            Vote vote = voteService.addVote(ownerId, trackId, clientId);

            // Her yeni oy sonrası anlık olarak client’lara yayınla
            Map<String, Long> updatedVotes = voteService.getActiveVotes(ownerId);
            messagingTemplate.convertAndSend("/topic/votes/" + ownerId, updatedVotes);

            return ResponseEntity.ok(vote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/played")
    public ResponseEntity<?> trackPlayed(@RequestBody Map<String, String> payload) {
        String ownerId = payload.get("ownerId");
        String trackId = payload.get("trackId");
        voteService.resetVotesForPlayedTrack(ownerId, trackId);
        return ResponseEntity.ok(Map.of("message", "Votes reset for " + trackId));
    }

    @GetMapping("/votes/{ownerId}")
    public ResponseEntity<?> getVotes(@PathVariable String ownerId) {
        return ResponseEntity.ok(voteService.getActiveVotes(ownerId));
    }
}
