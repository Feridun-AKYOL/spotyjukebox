package org.bithub.model;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record TokenPersistingRequest(@NotBlank String userId, @NotBlank String accessToken, @NotBlank String refreshToken, Set<String> scopes) {
}
