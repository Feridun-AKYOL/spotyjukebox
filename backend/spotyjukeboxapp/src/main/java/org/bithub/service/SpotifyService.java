package org.bithub.service;

import lombok.RequiredArgsConstructor;
import org.bithub.model.SpotifyDevice;
import org.bithub.model.SpotifyPlaylist;
import org.bithub.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final SpotifyRefreshService spotifyRefreshService;
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

            System.out.println("✅ Spotify token refreshed for user: " + user.getSpotifyUserId());
            return newAccessToken;
        }

        System.err.println("❌ Spotify token refresh failed for user: " + user.getSpotifyUserId());
        return null;
    }

    public List<SpotifyDevice> getAvailableDevices(UserInfo user) {
        String url = "https://api.spotify.com/v1/me/player/devices";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            System.out.println("Spotify devices API body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> devicesData = (List<Map<String, Object>>) response.getBody().get("devices");

                return devicesData.stream()
                        .map(d -> new SpotifyDevice(
                                (String) d.get("id"),
                                (String) d.get("name"),
                                (String) d.get("type"),
                                Boolean.TRUE.equals(d.get("is_active"))
                        ))
                        .collect(Collectors.toList());
            } else {
                throw new RuntimeException("Failed to fetch devices from Spotify API");
            }

        } catch (HttpClientErrorException.Unauthorized e) {
            // Token expired — refresh
            System.out.println("Access token expired, refreshing...");
            System.out.println("Old token: " + user.getAccessToken());
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            System.out.println("New token: " + refreshed.getAccessToken());

            if (refreshed == null || refreshed.getAccessToken()==null) {
                throw new RuntimeException("Failed to refresh access token");
            }
            return getAvailableDevices(refreshed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching Spotify devices");
        }
    }


}
