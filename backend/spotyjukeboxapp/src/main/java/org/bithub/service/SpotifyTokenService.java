//package org.bithub.service;
//
//import org.bithub.model.SpotifyToken;
//import org.bithub.persistence.SpotifyTokenRepository;
//import org.bithub.model.UserInfo;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.Instant;
//import java.util.Map;
//import java.util.Optional;
//
//@Service
//public class SpotifyTokenService {
//
//    private final SpotifyTokenRepository repository;
//    private final UserService userService;
//
//    @Value("${spotify.client-id}")
//    private String clientId;
//
//    @Value("${spotify.client-secret}")
//    private String clientSecret;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public SpotifyTokenService(SpotifyTokenRepository repository, UserService userService) {
//        this.repository = repository;
//        this.userService = userService;
//    }
//
//    public void saveTokens(UserInfo user, String accessToken, String refreshToken, long expiresIn) {
//        user.setAccessToken(accessToken);
//        if (refreshToken != null && !refreshToken.isEmpty()) {
//            user.setRefreshToken(refreshToken);
//        }
//        user.setExpiresIn(expiresIn);
//        userService.save(user);
//        System.out.println("💾 Tokens saved for user: " + user.getUserId());
//    }
//
//    public String refreshAccessToken(UserInfo user) {
//        String url = "https://accounts.spotify.com/api/token";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        headers.setBasicAuth(clientId, clientSecret);
//
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("grant_type", "refresh_token");
//        params.add("refresh_token", user.getRefreshToken());
//
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
//        RestTemplate restTemplate = new RestTemplate();
//
//        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
//
//        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//            String newAccessToken = (String) response.getBody().get("access_token");
//            user.setAccessToken(newAccessToken);
//            userService.save(user);
//            System.out.println("✅ Access token refreshed for user: " + user.getUserId());
//            return newAccessToken;
//        }
//
//        System.err.println("❌ Failed to refresh token for user: " + user.getUserId());
//        return null;
//    }
//
//
//    public Optional<SpotifyToken> getTokenByUser(UserInfo user) {
//        return repository.findByUserId(user.getId());
//    }
//}
