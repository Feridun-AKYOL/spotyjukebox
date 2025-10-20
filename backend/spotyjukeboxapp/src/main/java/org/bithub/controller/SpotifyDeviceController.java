package org.bithub.controller;

import lombok.RequiredArgsConstructor;
import org.bithub.model.SpotifyDevice;
import org.bithub.model.UserInfo;
import org.bithub.service.SpotifyService;
import org.bithub.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spotify")
@CrossOrigin(origins = "http://127.0.0.1:3000")
@RequiredArgsConstructor
public class SpotifyDeviceController {

    private final SpotifyService spotifyService;
    private final UserService userService;

    @GetMapping("/devices/{spotifyUserId}")
    public ResponseEntity<?> getDevices(@PathVariable("spotifyUserId") String userSpotifyId) {
        try {
            UserInfo user = userService.getUserBySpotifyId(userSpotifyId);
            System.out.println("Fetched user: " + user);
            System.out.println("Entering getAvailableDevices for user: " + user.getSpotifyUserId());
            System.out.println("Access token: " + user.getAccessToken());

            List<SpotifyDevice> devices = spotifyService.getAvailableDevices(user);
            System.out.println("Devices: " + devices);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to fetch devices");
        }
    }

    @PostMapping("/play")
    public ResponseEntity<?> playPlaylist(@RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String deviceId = body.get("deviceId");
            String playlistId = body.get("playlistId");

            if (userId == null || deviceId == null || playlistId == null) {
                return ResponseEntity.badRequest().body("Missing parameters");
            }

            UserInfo user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // 🎵 Playlist çalmayı başlat
            spotifyService.playOnDevice(user, deviceId, playlistId);

            // ✅ Kullanıcıya bu playlist'i Jukebox olarak bağla
            if (user.getJukeboxPlaylistId() == null || user.getJukeboxPlaylistId().isEmpty()) {
                user.setJukeboxPlaylistId(playlistId);
                userService.save(user);
                System.out.println("✅ Linked Jukebox playlist to user: " + playlistId);
            } else if (!user.getJukeboxPlaylistId().equals(playlistId)) {
                // Eğer kullanıcı başka bir listeyi seçtiyse, güncelle
                user.setJukeboxPlaylistId(playlistId);
                userService.save(user);
                System.out.println("🔁 Updated Jukebox playlist to: " + playlistId);
            }

            return ResponseEntity.ok(Map.of(
                    "status", "playing",
                    "linkedPlaylist", playlistId
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to play playlist");
        }
    }

    @GetMapping("/now-playing/{ownerId}")
    public ResponseEntity<?> getNowPlaying(@PathVariable String ownerId) {
        try {
            UserInfo user = userService.getUserBySpotifyId(ownerId);
            if (user == null) return ResponseEntity.notFound().build();

            Map<String, Object> data = spotifyService.getNowPlaying(user);

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to fetch now playing track");
        }
    }

}
