package org.bithub.persistence;

import org.bithub.model.PlayedSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayedSongRepository extends JpaRepository<PlayedSong, Long> {

    @Query("SELECT p.trackId FROM PlayedSong p WHERE p.ownerId = :ownerId ORDER BY p.playedAt DESC LIMIT 3")
    List<String> findLast3Songs(@Param("ownerId") String ownerId);
}
