package org.bithub.service;

import lombok.RequiredArgsConstructor;
import org.bithub.model.SpotifyDevice;
import org.bithub.model.SpotifyPlaylist;
import org.bithub.model.TrackVote;
import org.bithub.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
    private final VoteService voteService;

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


    public void playOnDevice(UserInfo user, String deviceId, String playlistId) {
        String url = "https://api.spotify.com/v1/me/player/play?device_id=" + deviceId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "context_uri", "spotify:playlist:" + playlistId
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            System.out.println("🎵 Playing playlist " + playlistId + " on device " + deviceId);
        } catch (HttpClientErrorException.Unauthorized e) {
            System.out.println("Access token expired. Refreshing...");
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            playOnDevice(refreshed, deviceId, playlistId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start playback");
        }
    }

    public Map<String, Object> getNowPlaying(UserInfo user) {
        String url = "https://api.spotify.com/v1/me/player/currently-playing";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                return Map.of("is_playing", false);
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            System.out.println("Access token expired. Refreshing...");
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            return getNowPlaying(refreshed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching now playing");
        }
    }

    public Map<String, Object> getQueue(UserInfo user) {
        String url = "https://api.spotify.com/v1/me/player/queue";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> queue = (List<Map<String, Object>>) body.get("queue");

                if (queue == null || queue.isEmpty()) {
                    System.out.println("No tracks found in queue");
                    return body;
                }

                // 🔹 Oy sayısını çek (sadece 1 saat içindekiler)
                Map<String, Long> voteCounts = voteService.getActiveVotes(user.getSpotifyUserId());
                List<String> cooldownTracks = voteService.getCooldownTracks(user.getSpotifyUserId());

                // 🔹 Oy bilgilerini queue’ya ekle
                queue.forEach(track -> {
                    String trackId = (String) track.get("id");
                    long votes = voteCounts.getOrDefault(trackId, 0L);
                    track.put("votes", votes);
                });

                // 🔹 Oy sayısına göre sırala (çok oyu olan öne)
                queue.sort((a, b) -> {
                    long v1 = (long) a.getOrDefault("votes", 0L);
                    long v2 = (long) b.getOrDefault("votes", 0L);
                    return Long.compare(v2, v1);
                });

                // 🔹 Cooldown’daki parçaları (son 3 çalan) sona at
                queue.sort((a, b) -> {
                    boolean aCooldown = cooldownTracks.contains(a.get("id"));
                    boolean bCooldown = cooldownTracks.contains(b.get("id"));
                    if (aCooldown && !bCooldown) return 1;
                    if (!aCooldown && bCooldown) return -1;
                    return 0;
                });

                body.put("queue", queue);
                return body;
            } else {
                throw new RuntimeException("No queue data returned from Spotify");
            }

        } catch (HttpClientErrorException.Unauthorized e) {
            System.out.println("Access token expired. Refreshing...");
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            return getQueue(refreshed);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching Spotify queue");
        }
    }

    public void overrideQueue(UserInfo user, List<TrackVote> rankedTracks) {
        String playUrl = "https://api.spotify.com/v1/me/player/play";
        String queueUrl = "https://api.spotify.com/v1/me/player/queue";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1️⃣ En çok oy alan şarkıyı hemen çal
        if (!rankedTracks.isEmpty()) {
            Map<String, Object> body = Map.of("uris", List.of("spotify:track:" + rankedTracks.get(0).trackId()));
            restTemplate.exchange(playUrl, HttpMethod.PUT, new HttpEntity<>(body, headers), Void.class);
        }

        // 2️⃣ Kalan şarkıları sırayla kuyruğa ekle
        for (int i = 1; i < rankedTracks.size(); i++) {
            String uri = "spotify:track:" + rankedTracks.get(i).trackId();
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(queueUrl)
                    .queryParam("uri", uri);
            restTemplate.exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        }

        System.out.println("✅ Spotify queue overridden by vote ranking.");
    }


}
