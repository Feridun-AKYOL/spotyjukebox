package org.bithub.validate;

import org.bithub.model.TokenPersistingRequest;

public class TokenPersistRequestValidator {

    public static void validate(TokenPersistingRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.accessToken() == null || request.accessToken().isBlank()) {
            throw new IllegalArgumentException("accessToken is required");
        }
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new IllegalArgumentException("refreshToken is required");
        }
    }
}
