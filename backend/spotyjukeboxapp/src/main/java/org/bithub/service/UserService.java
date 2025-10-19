package org.bithub.service;

import org.bithub.mapper.UserMapper;
import org.bithub.model.TokenPersistingRequest;
import org.bithub.model.UserInfo;
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
                        .spotifyUserId(request.userId())
                        .email(request.email())
                        .build());

        // mapper yalnızca değişen alanları günceller
        UserMapper.updateEntity(entity, request);

        return userInfoRepository.save(entity);
    }

    public UserInfo getById(String userId) {
        return userInfoRepository.findBySpotifyUserId(userId).orElse(null);
    }

    public UserInfo getByEmail(String email) {
        return userInfoRepository.findByEmail(email).orElse(null);
    }

    public List<UserInfo> findAll() {
        return userInfoRepository.findAll();
    }

    public UserInfo get(String userId) {
        return userInfoRepository.findBySpotifyUserId(userId).orElse(null);
    }


    public UserInfo findByEmail(String email) {
        return userInfoRepository.findByEmail(email).orElse(null);
    }

    public UserInfo findByRefreshToken(String refreshToken) {
        return userInfoRepository.findByRefreshToken(refreshToken).orElse(null);
    }

    public UserInfo getUserById(String userId) {
        return userInfoRepository.findBySpotifyUserId(userId).orElse(null);
    }

    public UserInfo getUserBySpotifyId(String userSpotifyId) {
        return userInfoRepository.findBySpotifyUserId(userSpotifyId).orElse(null);
    }


    public UserInfo findBySpotifyUserId(String spotifyUserId) {
        return userInfoRepository.findBySpotifyUserId(spotifyUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + spotifyUserId));
    }

    public void save(UserInfo user) {
        userInfoRepository.save(user);
    }

    /**
     * 🎵 Aktif jukebox oturumu olan kullanıcıları bulur
     */
    public List<UserInfo> findAllActiveJukeboxUsers() {
        // Aktif jukebox oturumu olan kullanıcıları bul
        // Örnek: Son 30 dakikada oylanmış veya şu an çalan kullanıcılar

        // TODO: Gerçek implementasyon - örnek:
        // return userRepository.findByJukeboxActiveTrue();
        // veya
        // return userRepository.findByLastActivityAfter(LocalDateTime.now().minusMinutes(30));

        return userInfoRepository.findAll(); // Geçici: Tüm kullanıcılar
    }
}
