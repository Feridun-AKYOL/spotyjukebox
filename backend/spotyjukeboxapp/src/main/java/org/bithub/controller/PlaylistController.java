package org.bithub.controller;

import org.bithub.model.SpotifyPlaylist;
import org.bithub.model.UserInfo;
import org.bithub.service.SpotifyService;
import org.bithub.service.UserService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://127.0.0.1:3000") // Adjust this according to your frontend URL
public class PlaylistController {

    private final SpotifyService spotifyService;
    private final UserService userService;

    public PlaylistController(SpotifyService spotifyService, UserService userService) {
        this.spotifyService = spotifyService;
        this.userService = userService;
    }

    @GetMapping("/playlists")
    public ResponseEntity<List<SpotifyPlaylist>> getPlaylists() {
        try {
            List<SpotifyPlaylist> playlists = spotifyService.getUserPlaylists();
            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/playlists/{userId}")
    public ResponseEntity<?> getUserPlaylists(@PathVariable String userId) {
        UserInfo user = userService.get(userId);
        if (user == null || user.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not connected to Spotify");
        }

        String accessToken = user.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.spotify.com/v1/me/playlists",
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            // TODO: burada refresh_token akışını ekle
        }

        return ResponseEntity.ok(response.getBody());
    }

}
