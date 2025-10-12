package org.bithub.controller;

import lombok.RequiredArgsConstructor;
import org.bithub.model.SpotifyDevice;
import org.bithub.model.UserInfo;
import org.bithub.service.SpotifyService;
import org.bithub.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
