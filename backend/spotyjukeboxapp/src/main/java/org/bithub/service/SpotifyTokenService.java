package org.bithub.service;

import org.bithub.model.SpotifyToken;
import org.bithub.persistence.SpotifyTokenRepository;
import org.bithub.model.UserInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class SpotifyTokenService {

    private final SpotifyTokenRepository repository;

    public SpotifyTokenService(SpotifyTokenRepository repository) {
        this.repository = repository;
    }

    public void saveTokens(UserInfo user, String accessToken, String refreshToken, long expiresIn) {
        Optional<SpotifyToken> existing = repository.findByUserId(user.getId());
        SpotifyToken token = existing.orElse(new SpotifyToken());
        token.setUser(user);
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        repository.save(token);
    }

    public Optional<SpotifyToken> getTokenByUser(UserInfo user) {
        return repository.findByUserId(user.getId());
    }
}
