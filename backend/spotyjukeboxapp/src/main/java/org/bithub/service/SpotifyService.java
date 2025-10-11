package org.bithub.service;

import lombok.RequiredArgsConstructor;
import org.bithub.model.SpotifyPlaylist;
import org.bithub.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpotifyService {

    @Value("${spotify.api.url}")
    private String spotifyApiUrl;

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final UserService userService;

    public List<SpotifyPlaylist> getUserPlaylists() {
        try {
            // TODO: Implement actual Spotify API call
            // This is a placeholder that will be implemented with actual Spotify API integration
            // let accessToken = localStorage.getItem('access_token');
            //
            //  const response = await fetch('https://api.spotify.com/v1/me', {
            //    headers: {
            //      Authorization: 'Bearer ' + accessToken
            //    }
            //  });
            //
            //  const data = await response.json();

            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch playlists", e);
        }
    }
    public String refreshAccessToken(UserInfo user) {
        String url = "https://accounts.spotify.com/api/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", user.getRefreshToken());

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String newAccessToken = (String) response.getBody().get("access_token");
            Number expiresIn = (Number) response.getBody().get("expires_in");

            user.setAccessToken(newAccessToken);
            user.setExpiresIn(expiresIn.longValue());
            userService.save(user);

            System.out.println("✅ Spotify token refreshed for user: " + user.getUserId());
            return newAccessToken;
        }

        System.err.println("❌ Spotify token refresh failed for user: " + user.getUserId());
        return null;
    }

}
