package org.bithub.mapper;

import org.bithub.model.TokenPersistingRequest;
import org.bithub.model.UserInfo;

import java.util.Set;

public class UserMapper {

    /**
     * Var olan entity'yi request'e göre günceller.
     * Boş/null değerlerde mevcut değeri korumak istiyorsan burada kontrol ekleyebilirsin.
     */
    public static void updateEntity(UserInfo entity, TokenPersistingRequest request) {
        entity.setUserId(request.userId());
        entity.setAccessToken(request.accessToken());
        entity.setRefreshToken(request.refreshToken());

        entity.setEmail(request.email());
        entity.setDisplayName(request.displayName());

        // scopes null gelebilir; null ise boş set atayalım ki NPE olmasın
        Set<String> scopes = request.scopes();
        entity.setScopes(scopes != null ? scopes : Set.of());
    }
}
