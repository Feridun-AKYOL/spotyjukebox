package org.bithub.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Represents a Spotify user within the Jukebox system.
 * Stores authentication tokens, profile details, and metadata
 * required to manage playback sessions and authorization.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "user_info",
        indexes = @Index(name = "ux_user_info_user_id", columnList = "spotify_user_id", unique = true)
)
public class UserInfo {

    /** Auto-generated primary key for the user record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The unique Spotify user ID associated with this account. */
    @Column(name = "spotify_user_id", nullable = false)
    private String spotifyUserId;

    /** The user's Spotify account email. */
    private String email;

    /** The display name of the user on Spotify. */
    private String displayName;

    /** The OAuth access token used for authenticated Spotify API calls. */
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    /** The OAuth refresh token used to renew access when the access token expires. */
    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    /** Timestamp for when the user record was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp for when the user record was last updated. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Duration (in seconds) for which the access token remains valid. */
    @Column(name = "expires_in", nullable = false)
    private Long expiresIn;

    /** The Spotify playlist ID currently linked as the user’s active Jukebox playlist. */
    @Column(name = "jukebox_playlist_id")
    private String jukeboxPlaylistId;

    /** The set of OAuth scopes granted to this user by Spotify. */
    @ElementCollection
    @CollectionTable(
            name = "user_info_scopes",
            joinColumns = @JoinColumn(name = "user_info_id")
    )
    @Column(name = "scope")
    private Set<String> scopes;

    /** Automatically sets creation and update timestamps before persisting. */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /** Automatically updates the {@code updatedAt} timestamp before modification. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
