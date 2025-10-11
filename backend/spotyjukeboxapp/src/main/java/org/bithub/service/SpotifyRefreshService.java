package org.bithub.service;

import lombok.RequiredArgsConstructor;
import org.bithub.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpotifyRefreshService {

    private final UserService userService;

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    /**
     * Spotify'dan yeni access_token alır ve veritabanını günceller.
     */
    public UserInfo refreshAccessToken(UserInfo user) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", user.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://accounts.spotify.com/api/token",
                    request,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("⚠️ Refresh token failed: " + response.getStatusCode());
                return null;
            }

            Map<String, Object> data = response.getBody();
            String newAccessToken = (String) data.get("access_token");
            Number expiresIn = (Number) data.get("expires_in");

            user.setAccessToken(newAccessToken);
            user.setExpiresIn(expiresIn != null ? expiresIn.longValue() : 3600L);
            user.setUpdatedAt(LocalDateTime.now());

            userService.save(user);
            System.out.println("✅ Token refreshed successfully for user: " + user.getUserId());

            return user;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
