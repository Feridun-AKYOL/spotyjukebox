package org.bithub.controller;

import org.bithub.model.SpotifyPlaylist;
import org.bithub.service.SpotifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000") // Adjust this according to your frontend URL
public class PlaylistController {

    private final SpotifyService spotifyService;

    public PlaylistController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
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
}
