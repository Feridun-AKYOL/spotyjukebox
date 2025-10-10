package org.bithub.controller;

import lombok.RequiredArgsConstructor;
import org.bithub.model.UserInfo;
import org.bithub.persistence.UserInfoRepository;
import org.bithub.service.SpotifyTokenService;
import org.bithub.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/spotify")
public class SpotifyAuthController {

  private final UserService userService;
    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    private final SpotifyTokenService spotifyTokenService;

    public SpotifyAuthController(SpotifyTokenService spotifyTokenService, UserInfoRepository userInfoRepository, UserService userService) {
        this.spotifyTokenService = spotifyTokenService;
        this.userService = userService;
    }

    @PostMapping("/callback")
    public ResponseEntity<?> handleSpotifyCallback(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String userId = body.get("userId");

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("Missing authorization code");
        }

        System.out.println("🔁 Spotify code received: " + code);
        System.out.println("🎯 Redirect URI: " + redirectUri);

        try {
            // Step 1: Token exchange
            String tokenUrl = "https://accounts.spotify.com/api/token";

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                System.err.println("❌ Spotify token exchange failed: " + tokenResponse);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Spotify token exchange failed");
            }

            Map<String, Object> tokenData = tokenResponse.getBody();
            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");
            Number expiresIn = (Number) tokenData.get("expires_in");

            System.out.println("✅ Token exchange success. Access token received.");

            // Step 2: Fetch user profile
            HttpHeaders profileHeaders = new HttpHeaders();
            profileHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeaders);

            ResponseEntity<Map> profileResponse = restTemplate.exchange(
                    "https://api.spotify.com/v1/me",
                    HttpMethod.GET,
                    profileRequest,
                    Map.class
            );

            if (!profileResponse.getStatusCode().is2xxSuccessful() || profileResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Failed to fetch Spotify user profile");
            }

            Map<String, Object> profileData = profileResponse.getBody();
            String spotifyUserId = (String) profileData.get("id");
            String displayName = (String) profileData.get("display_name");
            String email = (String) profileData.get("email");

            System.out.println("🎵 Spotify user: " + displayName + " (" + spotifyUserId + ")");

            // Step 3: Save or update user
            UserInfo user = userService.get(spotifyUserId);
            if (user == null) {
                user = new UserInfo();
                user.setUserId(spotifyUserId);
            }
            user.setDisplayName(displayName);
            user.setEmail(email);
            user.setAccessToken(accessToken);
            user.setRefreshToken(refreshToken);
            user.setExpiresIn(expiresIn != null ? expiresIn.longValue() : 0L);

            userService.save(user);

            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "userId", spotifyUserId,
                    "accessToken", accessToken,
                    "accessTokenSaved", true
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Spotify auth error: " + e.getMessage());
        }
    }



    @GetMapping("/login")
    public ResponseEntity<?> redirectToSpotifyAuth() {
        String scopes = "user-read-private playlist-read-private";
        String authorizeUrl = "https://accounts.spotify.com/authorize" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=" + redirectUri +
                "&scope=" + scopes +
                "&show_dialog=true";
        return ResponseEntity.status(302).header("Location", authorizeUrl).build();
    }

}
