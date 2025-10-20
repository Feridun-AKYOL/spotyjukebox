package org.bithub.controller;

import jakarta.validation.Valid;
import org.bithub.model.TokenPersistingRequest;
import org.bithub.model.UserInfo;
import org.bithub.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing user data and registration.
 * Provides endpoints for user creation, lookup, and listing.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    /**
     * Registers a new user or updates an existing one using the provided token data.
     *
     * @param request the token and user data to persist
     * @return a success response containing the user's Spotify ID
     */
    @PostMapping("/register")
    public ResponseEntity<?> persist(@Valid @RequestBody TokenPersistingRequest request) {
        UserInfo saved = service.persistOrUpdate(request);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "userId", saved.getSpotifyUserId()
        ));
    }

    /**
     * Retrieves user information by Spotify user ID.
     *
     * @param userId the Spotify user ID
     * @return the user information or an error message if not found
     */
    @GetMapping("/get/{userId}")
    public ResponseEntity<?> get(@PathVariable String userId) {
        UserInfo user = service.getById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", "User not found"
            ));
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Retrieves user information by email address.
     *
     * @param email the user's email address
     * @return the user information or an error message if not found
     */
    @GetMapping("/get-by-email/{email}")
    public ResponseEntity<?> getByEmail(@PathVariable String email) {
        UserInfo user = service.getByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", "User not found with email: " + email
            ));
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Retrieves a list of all users in the system.
     *
     * @return a list of {@link UserInfo} objects
     */
    @GetMapping("/list")
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(service.findAll());
    }
}
