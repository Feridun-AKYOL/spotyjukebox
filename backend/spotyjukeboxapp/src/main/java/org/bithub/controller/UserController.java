package org.bithub.controller;

import jakarta.validation.Valid;
import org.bithub.model.TokenPersistingRequest;
import org.bithub.persistence.UserInfo;
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
        return ResponseEntity.ok(Map.of("status","ok","userId", saved.getUserId()));
    }

    @GetMapping("/get/{userId}")
    public ResponseEntity<?> get(@PathVariable String userId) {
        UserInfo u = service.get(userId);
        if (u == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error","NOT_FOUND",
                    "message","User not found"
            ));
        }
        return ResponseEntity.ok(u);
    }
}
