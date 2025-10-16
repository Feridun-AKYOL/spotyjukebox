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
            spotifyService.playOnDevice(user, deviceId, playlistId);

            return ResponseEntity.ok(Map.of("status", "playing"));
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
