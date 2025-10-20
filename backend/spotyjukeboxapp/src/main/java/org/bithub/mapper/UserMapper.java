package org.bithub.mapper;

import org.bithub.model.TokenPersistingRequest;
import org.bithub.model.UserInfo;

import java.util.Set;

/**
 * Utility class for mapping user-related data between DTOs and entities.
 */
public class UserMapper {

    /**
     * Updates an existing {@link UserInfo} entity based on data from a {@link TokenPersistingRequest}.
     * If any fields in the request are null, the corresponding entity fields can be conditionally preserved
     * (this can be extended in the future for partial updates).
     *
     * @param entity  the existing {@link UserInfo} entity to update
     * @param request the incoming token data from the client
     */
    public static void updateEntity(UserInfo entity, TokenPersistingRequest request) {
        entity.setSpotifyUserId(request.userId());
        entity.setAccessToken(request.accessToken());
        entity.setRefreshToken(request.refreshToken());
        entity.setEmail(request.email());
        entity.setDisplayName(request.displayName());

        // Prevent NullPointerException if scopes are null
        Set<String> scopes = request.scopes();
        entity.setScopes(scopes != null ? scopes : Set.of());
    }
}
