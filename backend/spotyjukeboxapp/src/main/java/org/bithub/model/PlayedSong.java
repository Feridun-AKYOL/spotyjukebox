package org.bithub.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Represents a record of a song played in the Jukebox system.
 * Each entry stores the Spotify track, its owner, and the timestamp of when it was played.
 */
@Entity
@Table(name = "played_songs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayedSong {

    /** Unique identifier for the played song record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Spotify user ID who owns the Jukebox session. */
    private String ownerId;

    /** The Spotify track ID that was played. */
    private String trackId;

    /** Timestamp of when the track was played. */
    private LocalDateTime playedAt;

    /**
     * Automatically sets the {@code playedAt} timestamp before the entity is persisted.
     */
    @PrePersist
    public void onCreate() {
        this.playedAt = LocalDateTime.now();
    }
}
