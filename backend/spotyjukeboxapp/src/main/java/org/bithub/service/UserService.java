package org.bithub.service;

import org.bithub.mapper.UserMapper;
import org.bithub.model.TokenPersistingRequest;
import org.bithub.persistence.UserInfo;
import org.bithub.persistence.UserInfoRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserInfoRepository userInfoRepository;

    public UserService(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    public void persist(TokenPersistingRequest request) {
        var entity = UserMapper.map(request);
        userInfoRepository.save(entity);
    }

    public UserInfo get(String userId) {
        return userInfoRepository.getUserInfoByUserId(userId);
    }
}
