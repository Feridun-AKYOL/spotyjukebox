package org.bithub.model;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

/**
 * Represents a request object for persisting or updating a Spotify user's authentication tokens.
 * Used when registering or refreshing a user's credentials in the system.
 *
 * @param userId       the Spotify user ID
 * @param accessToken  the OAuth access token issued by Spotify
 * @param refreshToken the OAuth refresh token used to renew access
 * @param email        the user's Spotify account email
 * @param displayName  the display name of the user on Spotify
 * @param scopes       the set of OAuth scopes granted to the application
 */
public record TokenPersistingRequest(
        @NotBlank String userId,
        @NotBlank String accessToken,
        @NotBlank String refreshToken,
        @NotBlank String email,
        @NotBlank String displayName,
        Set<String> scopes
) {}
