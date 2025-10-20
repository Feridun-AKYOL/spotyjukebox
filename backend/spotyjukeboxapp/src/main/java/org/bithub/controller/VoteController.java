package org.bithub.controller;

import lombok.RequiredArgsConstructor;
import org.bithub.model.Vote;
import org.bithub.service.VoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing voting actions in the Jukebox system.
 * Handles track voting, vote broadcasting, and resetting votes when a track is played.
 */
@RestController
@RequestMapping("/api/jukebox")
@CrossOrigin(
        origins = {"http://127.0.0.1:5173", "http://localhost:5173"},
        allowCredentials = "true"
)
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Registers a vote for a specific track and broadcasts the updated vote count
     * to all connected clients via WebSocket.
     *
     * @param payload the request body containing ownerId, trackId, and optionally clientId
     * @return the created {@link Vote} object or an error message
     */
    @PostMapping("/vote")
    public ResponseEntity<?> vote(@RequestBody Map<String, String> payload) {
        try {
            String ownerId = payload.get("ownerId");
            String trackId = payload.get("trackId");
            String clientId = payload.getOrDefault("clientId", UUID.randomUUID().toString());

            Vote vote = voteService.addVote(ownerId, trackId, clientId);

            // Broadcast updated votes to all clients in real time
            Map<String, Long> updatedVotes = voteService.getActiveVotes(ownerId);
            messagingTemplate.convertAndSend("/topic/votes/" + ownerId, updatedVotes);

            return ResponseEntity.ok(vote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Resets votes for a track after it has been played.
     *
     * @param payload the request body containing ownerId and trackId
     * @return a confirmation message or an error response
     */
    @PostMapping("/played")
    public ResponseEntity<?> trackPlayed(@RequestBody Map<String, String> payload) {
        try {
            String ownerId = payload.get("ownerId");
            String trackId = payload.get("trackId");
            voteService.resetVotesForPlayedTrack(ownerId, trackId);

            return ResponseEntity.ok(Map.of("message", "Votes reset for track: " + trackId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retrieves the current active votes for a given owner (Jukebox session).
     *
     * @param ownerId the Spotify owner ID
     * @return a map of track IDs and their corresponding vote counts
     */
    @GetMapping("/votes/{ownerId}")
    public ResponseEntity<?> getVotes(@PathVariable String ownerId) {
        return ResponseEntity.ok(voteService.getActiveVotes(ownerId));
    }
}
