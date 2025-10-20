package org.bithub.persistence;

import jakarta.transaction.Transactional;
import org.bithub.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing {@link Vote} entities.
 * Provides custom queries for retrieving and maintaining vote data
 * related to Jukebox sessions.
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    /**
     * Retrieves vote counts for all tracks belonging to a specific Jukebox session owner,
     * filtered by a given timestamp. Returns a list of track IDs and their corresponding
     * vote counts, ordered by vote count in descending order.
     *
     * @param ownerId the Spotify user ID representing the session owner (DJ)
     * @param since   the minimum timestamp for votes to be included in the count
     * @return a list of object arrays containing track IDs and their vote counts
     */
    @Query("SELECT v.trackId, COUNT(v) FROM Vote v " +
            "WHERE v.ownerId = :ownerId AND v.createdAt > :since " +
            "GROUP BY v.trackId ORDER BY COUNT(v) DESC")
    List<Object[]> findRecentVoteCounts(@Param("ownerId") String ownerId,
                                        @Param("since") LocalDateTime since);

    /**
     * Deletes all votes for a specific track within a given Jukebox session.
     *
     * @param ownerId the Spotify user ID representing the session owner
     * @param trackId the Spotify track ID for which votes should be removed
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Vote v WHERE v.ownerId = :ownerId AND v.trackId = :trackId")
    void deleteVotesForTrack(@Param("ownerId") String ownerId, @Param("trackId") String trackId);

    /**
     * Deletes all votes created before a specific timestamp.
     * Useful for periodic cleanup to prevent data buildup.
     *
     * @param threshold the cutoff time for old votes to be deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Vote v WHERE v.createdAt < :threshold")
    void deleteOldVotes(@Param("threshold") LocalDateTime threshold);

    /**
     * Finds all votes associated with a specific track within a Jukebox session.
     *
     * @param ownerId the Spotify user ID representing the session owner
     * @param trackId the Spotify track ID
     * @return a list of votes matching the owner and track
     */
    List<Vote> findByOwnerIdAndTrackId(String ownerId, String trackId);
}
