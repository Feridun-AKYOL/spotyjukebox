package org.bithub.persistence;

import org.bithub.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing and managing {@link UserInfo} entities.
 * Provides convenient query methods for finding users by Spotify identifiers,
 * email, refresh tokens, and Jukebox playlist associations.
 */
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {

    /**
     * Finds a user by their registered email address.
     *
     * @param email the user's email address
     * @return an {@link Optional} containing the user if found
     */
    Optional<UserInfo> findByEmail(String email);

    /**
     * Finds a user by their unique Spotify user ID.
     *
     * @param spotifyUserId the Spotify user ID
     * @return an {@link Optional} containing the user if found
     */
    Optional<UserInfo> findBySpotifyUserId(String spotifyUserId);

    /**
     * Finds a user by their stored refresh token.
     *
     * @param refreshToken the OAuth refresh token
     * @return an {@link Optional} containing the user if found
     */
    Optional<UserInfo> findByRefreshToken(String refreshToken);

    /**
     * Retrieves all users who have an active Jukebox playlist linked.
     *
     * @return a list of users with non-null Jukebox playlist IDs
     */
    List<UserInfo> findByJukeboxPlaylistIdIsNotNull();
}
