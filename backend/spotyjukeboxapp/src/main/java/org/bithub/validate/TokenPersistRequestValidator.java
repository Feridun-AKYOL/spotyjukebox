package org.bithub.validate;

import org.bithub.model.TokenPersistingRequest;

/**
 * TokenPersistRequestValidator
 * ------------------------------------------------------------------------
 * Performs validation on {@link TokenPersistingRequest} objects before
 * persisting or updating a Spotify user in the system.
 *
 * Responsibilities:
 *   • Ensure required fields (userId, accessToken, refreshToken) are present
 *   • Prevent incomplete or invalid requests from reaching persistence layer
 *
 * This class uses static validation for lightweight usage without instantiation.
 */
public class TokenPersistRequestValidator {

    /**
     * Validates a {@link TokenPersistingRequest} for required fields.
     *
     * @param request the token persistence request to validate
     * @throws IllegalArgumentException if any required field is missing or blank
     */
    public static void validate(TokenPersistingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        if (isBlank(request.userId())) {
            throw new IllegalArgumentException("Spotify user ID (userId) is required.");
        }

        if (isBlank(request.accessToken())) {
            throw new IllegalArgumentException("Access token (accessToken) is required.");
        }

        if (isBlank(request.refreshToken())) {
            throw new IllegalArgumentException("Refresh token (refreshToken) is required.");
        }
    }

    /**
     * Helper method to check for null or blank strings.
     *
     * @param value string to check
     * @return true if null or blank, false otherwise
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
