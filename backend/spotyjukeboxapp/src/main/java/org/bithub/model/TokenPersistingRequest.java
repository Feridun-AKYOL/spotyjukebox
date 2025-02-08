package org.bithub.model;

public record TokenPersistingRequest(String userId, String accessToken, String refreshToken) {
}
