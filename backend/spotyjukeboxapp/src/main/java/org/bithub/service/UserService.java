package org.bithub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bithub.mapper.UserMapper;
import org.bithub.model.TokenPersistingRequest;
import org.bithub.model.UserInfo;
import org.bithub.persistence.UserInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserService
 * ------------------------------------------------------------------------
 * Provides CRUD and persistence operations for {@link UserInfo} entities.
 * This service layer ensures idempotent user creation/update and safe token storage.
 *
 * Compatibility:
 *   - Keeps all legacy method names (getUserById, findByEmail, etc.)
 *   - Internally routes them to the unified core methods (getById, getByEmail)
 *
 * Responsibilities:
 *   • Create or update user records idempotently
 *   • Retrieve users by Spotify ID, email, or refresh token
 *   • Provide lists of active Jukebox users
 *   • Maintain backward compatibility for existing controllers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoRepository userInfoRepository;

    // --------------------------------------------------------------------
    // USER CREATION / UPDATE
    // --------------------------------------------------------------------

    /**
     * Backward compatible legacy entry point.
     * Delegates to {@link #persistOrUpdate(TokenPersistingRequest)}.
     *
     * @param request user token persistence request
     * @return persisted or updated {@link UserInfo}
     */
    @Transactional
    public UserInfo persist(TokenPersistingRequest request) {
        return persistOrUpdate(request);
    }

    /**
     * Creates or updates a user in an idempotent way.
     * <ul>
     *     <li>If the email exists, the record is updated with new token/scopes.</li>
     *     <li>If not found, a new user entry is created.</li>
     * </ul>
     *
     * @param request the incoming Spotify token request
     * @return the saved {@link UserInfo} entity
     */
    @Transactional
    public UserInfo persistOrUpdate(TokenPersistingRequest request) {
        UserInfo entity = userInfoRepository.findByEmail(request.email())
                .orElseGet(() -> UserInfo.builder()
                        .spotifyUserId(request.userId())
                        .email(request.email())
                        .build());

        // Mapper updates only changed fields (tokens, scopes, etc.)
        UserMapper.updateEntity(entity, request);

        UserInfo saved = userInfoRepository.save(entity);
        log.info("✅ Persisted or updated user: {}", saved.getSpotifyUserId());
        return saved;
    }


    // --------------------------------------------------------------------
    // CORE RETRIEVAL METHODS
    // --------------------------------------------------------------------

    /**
     * Fetches a user by Spotify user ID.
     *
     * @param userId Spotify user ID
     * @return matching {@link UserInfo}, or {@code null} if not found
     */
    public UserInfo getById(String userId) {
        return userInfoRepository.findBySpotifyUserId(userId).orElse(null);
    }

    /**
     * Fetches a user by email.
     *
     * @param email user email address
     * @return matching {@link UserInfo}, or {@code null} if not found
     */
    public UserInfo getByEmail(String email) {
        return userInfoRepository.findByEmail(email).orElse(null);
    }

    /**
     * Fetches a user by refresh token.
     *
     * @param refreshToken Spotify refresh token
     * @return matching {@link UserInfo}, or {@code null} if not found
     */
    public UserInfo findByRefreshToken(String refreshToken) {
        return userInfoRepository.findByRefreshToken(refreshToken).orElse(null);
    }

    /**
     * Fetches a user by Spotify user ID, throwing an exception if not found.
     *
     * @param spotifyUserId Spotify user ID
     * @return matching {@link UserInfo}
     * @throws RuntimeException if no user found
     */
    public UserInfo findBySpotifyUserId(String spotifyUserId) {
        return userInfoRepository.findBySpotifyUserId(spotifyUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + spotifyUserId));
    }

    /**
     * Saves (inserts or updates) the given {@link UserInfo}.
     *
     * @param user user entity
     */
    public void save(UserInfo user) {
        userInfoRepository.save(user);
    }

    /**
     * Fetches all users.
     *
     * @return list of all {@link UserInfo} entities
     */
    public List<UserInfo> findAll() {
        return userInfoRepository.findAll();
    }


    // --------------------------------------------------------------------
    // BACKWARD COMPATIBLE ALIASES
    // --------------------------------------------------------------------

    /**
     * Legacy alias for {@link #getById(String)}.
     */
    public UserInfo get(String userId) {
        return getById(userId);
    }

    /**
     * Legacy alias for {@link #getById(String)}.
     */
    public UserInfo getUserById(String userId) {
        return getById(userId);
    }

    /**
     * Legacy alias for {@link #getById(String)}.
     */
    public UserInfo getUserBySpotifyId(String userSpotifyId) {
        return getById(userSpotifyId);
    }

    /**
     * Legacy alias for {@link #getByEmail(String)}.
     */
    public UserInfo findByEmail(String email) {
        return getByEmail(email);
    }


    // --------------------------------------------------------------------
    // JUKEBOX SUPPORT
    // --------------------------------------------------------------------

    /**
     * Finds all users who currently have an active Jukebox session.
     * <p>
     * TODO: Implement logic to identify "active" users (e.g. recently played or voted).
     * For now, returns all users.
     * </p>
     *
     * @return list of active Jukebox users (placeholder implementation)
     */
    public List<UserInfo> findAllActiveJukeboxUsers() {
        log.debug("Returning placeholder list of all users as active Jukebox users");
        return userInfoRepository.findAll();
    }
}
