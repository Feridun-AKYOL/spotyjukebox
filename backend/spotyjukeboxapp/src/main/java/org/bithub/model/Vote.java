package org.bithub.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Represents a vote cast by a user (client) for a specific Spotify track
 * within a Jukebox session. Each vote is linked to a session owner (DJ)
 * and a unique client identifier.
 */
@Entity
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    /** Auto-generated primary key for the vote record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Spotify user ID representing the Jukebox session owner (DJ). */
    private String ownerId;

    /** The Spotify track ID that received the vote. */
    private String trackId;

    /** The unique client identifier (e.g., browser session ID) of the voter. */
    private String clientId;

    /** Timestamp indicating when the vote was created. */
    private LocalDateTime createdAt;

    /** Automatically sets the creation timestamp before persisting. */
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
