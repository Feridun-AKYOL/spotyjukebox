package org.bithub.persistence;

import org.bithub.model.SpotifyToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Component
public interface SpotifyTokenRepository extends JpaRepository<SpotifyToken, Long> {
    Optional<SpotifyToken> findByUserId(Long userId);
}
