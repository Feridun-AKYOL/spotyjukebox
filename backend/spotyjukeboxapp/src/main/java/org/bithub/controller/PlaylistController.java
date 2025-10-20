package org.bithub.controller;

import lombok.RequiredArgsConstructor;
import org.bithub.model.TrackVote;
import org.bithub.model.UserInfo;
import org.bithub.service.SpotifyService;
import org.bithub.service.UserService;
import org.bithub.service.SpotifyRefreshService;
import org.bithub.service.VoteService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class PlaylistController {

    private final UserService userService;
    private final SpotifyRefreshService spotifyRefreshService;
    private final SpotifyService spotifyService;
    private final VoteService voteService;

    @GetMapping("/playlists/{userId}")
    public ResponseEntity<?> getUserPlaylists(@PathVariable String userId) {
        UserInfo user = userService.get(userId);
        if (user == null || user.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User or access token not found"));
        }

        try {
            return fetchPlaylists(user);

        } catch (HttpClientErrorException.Unauthorized e) {
            System.out.println("⚠️ Access token expired, refreshing...");

            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            if (refreshed == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Failed to refresh token"));
            }

            try {
                return fetchPlaylists(refreshed);
            } catch (Exception retryEx) {
                retryEx.printStackTrace();
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Failed to fetch after refresh: " + retryEx.getMessage()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<?> fetchPlaylists(UserInfo user) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.spotify.com/v1/me/playlists?limit=20&offset=0",
                HttpMethod.GET,
                entity,
                Map.class
        );

        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/queue/{ownerId}")
    public ResponseEntity<?> getQueue(@PathVariable String ownerId) {
        try {
            UserInfo user = userService.getUserBySpotifyId(ownerId);
            if (user == null) return ResponseEntity.notFound().build();

            Map<String, Object> queueData = spotifyService.getQueue(user);
            return ResponseEntity.ok(queueData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to fetch Spotify queue");
        }
    }

//    @PostMapping("/queue/reorder")
//    public ResponseEntity<?> reorderQueue(@RequestBody Map<String, String> payload) {
//        String ownerId = payload.get("ownerId");
//
//        UserInfo user = userService.getUserBySpotifyId(ownerId);
//        List<TrackVote> rankedTracks = voteService.getRankedTracks(ownerId); // oya göre sıralı
//
//        spotifyService.overrideQueue(user);
//
//        return ResponseEntity.ok(Map.of("status", "Queue reordered on Spotify"));
//    }
@GetMapping("/upcoming-tracks/{ownerId}")
public ResponseEntity<?> getUpcomingTracks(@PathVariable String ownerId) {
    try {

        UserInfo user = userService.findBySpotifyUserId(ownerId);
        System.out.println("ownerId: " + ownerId);
        List<Map<String, Object>> tracks = spotifyService.getUpcomingTracksWithVotes(user);
        System.out.println("tracks: " + tracks);

        return ResponseEntity.ok(Map.of("queue", tracks));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

}
