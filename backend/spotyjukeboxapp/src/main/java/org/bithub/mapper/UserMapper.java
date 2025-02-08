package org.bithub.mapper;

import org.bithub.model.TokenPersistingRequest;
import org.bithub.persistence.UserInfo;

public class UserMapper {

    public static UserInfo map(TokenPersistingRequest request) {
        return UserInfo.builder()
                .userId(request.userId())
                .accessToken(request.accessToken())
                .refreshToken(request.refreshToken())
                .build();
    }
}
