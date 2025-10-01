package org.bithub.service;

import org.bithub.mapper.UserMapper;
import org.bithub.model.TokenPersistingRequest;
import org.bithub.persistence.UserInfo;
import org.bithub.persistence.UserInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserInfoRepository userInfoRepository;

    public UserService(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    /**
     * Backward compatible: mevcut controller'ın çağırdığı metot.
     * Artık idempotent olacak şekilde persistOrUpdate'e yönlendirir.
     */
    @Transactional
    public UserInfo persist(TokenPersistingRequest request) {
        return persistOrUpdate(request);
    }

    /**
     * Idempotent kayıt/güncelleme:
     * - userId mevcutsa: token/scopes güncellenir
     * - yoksa: yeni kayıt oluşturulur
     */
    @Transactional
    public UserInfo persistOrUpdate(TokenPersistingRequest request) {
        UserInfo entity = userInfoRepository.findByEmail(request.email())
                .orElseGet(() -> UserInfo.builder()
                        .userId(request.userId())
                        .email(request.email())
                        .build());

        // mapper yalnızca değişen alanları günceller
        UserMapper.updateEntity(entity, request);

        return userInfoRepository.save(entity);
    }

    public UserInfo getById(String userId) {
        return userInfoRepository.findByUserId(userId).orElse(null);
    }

    public UserInfo getByEmail(String email) {
        return userInfoRepository.findByEmail(email).orElse(null);
    }

    public List<UserInfo> findAll() {
        return userInfoRepository.findAll();
    }


}
