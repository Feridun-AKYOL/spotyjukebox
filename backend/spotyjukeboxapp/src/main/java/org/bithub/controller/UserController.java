package org.bithub.controller;

import jakarta.validation.Valid;
import org.bithub.model.TokenPersistingRequest;
import org.bithub.model.UserInfo;
import org.bithub.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @PostMapping("/register")
    public ResponseEntity<?> persist(@Valid @RequestBody TokenPersistingRequest request) {
        UserInfo saved = service.persistOrUpdate(request);
        return ResponseEntity.ok(Map.of("status","ok","userId", saved.getSpotifyUserId()));
    }

    @GetMapping("/get/{userId}")
    public ResponseEntity<?> get(@PathVariable String userId) {
        System.out.println("🔍 Looking for userId: " + userId); // DEBUG
        UserInfo u = service.getById(userId);
        if (u == null) {
            System.out.println("❌ User not found: " + userId); // DEBUG
            return ResponseEntity.status(404).body(Map.of(
                    "error","NOT_FOUND",
                    "message","User not found"
            ));
        }
        System.out.println("✅ User found: " + u.getSpotifyUserId()); // DEBUG
        return ResponseEntity.ok(u);
    }

    @GetMapping("/get-by-email/{email}")
    public ResponseEntity<?> getByEmail(@PathVariable String email) {
        System.out.println("📧 GET request for email: " + email);
        UserInfo u = service.getByEmail(email);
        if (u == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error","NOT_FOUND",
                    "message","User not found with email: " + email
            ));
        }
        return ResponseEntity.ok(u);
    }

    @GetMapping("/list")
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(service.findAll());
    }
}
