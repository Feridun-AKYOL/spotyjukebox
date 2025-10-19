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
import java.util.*;
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
            System.out.println("Access token expired, refreshing...");
            System.out.println("Old token: " + user.getAccessToken());
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            System.out.println("New token: " + refreshed.getAccessToken());

            if (refreshed == null || refreshed.getAccessToken() == null) {
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

                // 🔹 Oy bilgilerini queue'ya ekle
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

                // 🔹 Cooldown'daki parçaları (son 3 çalan) sona at
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

    // ========== YÖNTEM 3: PLAYLIST BAZLI JUKEBOX ==========

    /**
     * 🎵 Kullanıcı için özel Jukebox playlist'i oluşturur
     */
    public String createJukeboxPlaylist(UserInfo user) {
        String url = "https://api.spotify.com/v1/users/" + user.getSpotifyUserId() + "/playlists";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "name", "🎵 Jukebox - " + System.currentTimeMillis(),
                "description", "Dynamic voting-based playlist",
                "public", false
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String playlistId = (String) response.getBody().get("id");
                System.out.println("✅ Jukebox playlist created: " + playlistId);

                // Kullanıcının jukebox playlist ID'sini kaydet
                user.setJukeboxPlaylistId(playlistId);
                userService.save(user);

                return playlistId;
            } else {
                throw new RuntimeException("Failed to create playlist");
            }

        } catch (HttpClientErrorException.Unauthorized e) {
            System.out.println("Access token expired. Refreshing...");
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            return createJukeboxPlaylist(refreshed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating Jukebox playlist");
        }
    }

    /**
     * 🎵 Mevcut playlist'in şarkılarını çeker
     */
    public List<Map<String, Object>> getPlaylistTracks(UserInfo user, String playlistId) {
        String url = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> items = (List) response.getBody().get("items");

                return items.stream()
                        .map(item -> (Map<String, Object>) item.get("track"))
                        .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }

        } catch (HttpClientErrorException.Unauthorized e) {
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            return getPlaylistTracks(refreshed, playlistId);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 🗳️ Playlist'i oylamaya göre yeniden sıralar
     */
    public void updateJukeboxPlaylist(UserInfo user) {
        try {
            String playlistId = user.getJukeboxPlaylistId();

            // ✅ Playlist yoksa sadece log bas ve geç (hata fırlatma)
            if (playlistId == null || playlistId.isEmpty()) {
                System.out.println("⏸️ No active jukebox for user: " + user.getSpotifyUserId());
                return; // ✅ Sessizce geç
            }

            // 1️⃣ Mevcut playlist şarkılarını çek
            List<Map<String, Object>> currentTracks = getPlaylistTracks(user, playlistId);

            if (currentTracks.isEmpty()) {
                System.out.println("⚠️ Jukebox playlist is empty, nothing to reorder");
                return;
            }

            // 2️⃣ Şarkıları oylamaya göre sırala
            List<String> orderedUris = sortPlaylistByVotes(user, currentTracks);

            if (orderedUris.isEmpty()) {
                System.out.println("⚠️ No valid tracks to reorder");
                return;
            }

            // 3️⃣ Playlist'i güncelle
            replacePlaylistTracks(user, playlistId, orderedUris);

            System.out.println("✅ Jukebox playlist updated: " + orderedUris.size() + " tracks reordered");

        } catch (Exception e) {
            System.err.println("❌ Failed to update jukebox playlist for: " + user.getSpotifyUserId());
            e.printStackTrace();
        }
    }

    /**
     * 🔄 Playlist şarkılarını oylamaya göre sıralar
     */
    private List<String> sortPlaylistByVotes(UserInfo user, List<Map<String, Object>> tracks) {
        // Oy sayılarını ve cooldown listesini çek
        Map<String, Long> voteCounts = voteService.getActiveVotes(user.getSpotifyUserId());
        List<String> cooldownTracks = voteService.getCooldownTracks(user.getSpotifyUserId());

        // Şarkıları 3 kategoriye ayır
        List<Map<String, Object>> votedTracks = new ArrayList<>();
        List<Map<String, Object>> unvotedTracks = new ArrayList<>();
        List<Map<String, Object>> cooldownTracksInPlaylist = new ArrayList<>();

        for (Map<String, Object> track : tracks) {
            String trackId = (String) track.get("id");
            String uri = (String) track.get("uri");

            if (trackId == null || uri == null) continue;

            if (cooldownTracks.contains(trackId)) {
                cooldownTracksInPlaylist.add(track); // Son 3 çalan → en sona
            } else if (voteCounts.containsKey(trackId) && voteCounts.get(trackId) > 0) {
                track.put("votes", voteCounts.get(trackId));
                votedTracks.add(track); // Oylanmış → vote'a göre sırala
            } else {
                track.put("votes", 0L);
                unvotedTracks.add(track); // Oylanmamış → ortada
            }
        }

        // Oylanmış şarkıları vote sayısına göre sırala (çok oy → öne)
        votedTracks.sort((a, b) -> {
            long v1 = (long) a.getOrDefault("votes", 0L);
            long v2 = (long) b.getOrDefault("votes", 0L);
            return Long.compare(v2, v1);
        });

        // Final sıralama: [Oylanmış (çok oy öne)] + [Oylanmamış] + [Cooldown]
        List<String> orderedUris = new ArrayList<>();
        votedTracks.forEach(t -> orderedUris.add((String) t.get("uri")));
        unvotedTracks.forEach(t -> orderedUris.add((String) t.get("uri")));
        cooldownTracksInPlaylist.forEach(t -> orderedUris.add((String) t.get("uri")));

        System.out.println("📊 Sorted playlist: " + votedTracks.size() + " voted, "
                + unvotedTracks.size() + " unvoted, "
                + cooldownTracksInPlaylist.size() + " cooldown");

        return orderedUris;
    }

    /**
     * 📝 Playlist'in tüm şarkılarını değiştirir
     */
    private void replacePlaylistTracks(UserInfo user, String playlistId, List<String> uris) {
        String url = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Spotify API limiti: 100 şarkı/istek
        int batchSize = 100;
        for (int i = 0; i < uris.size(); i += batchSize) {
            List<String> batch = uris.subList(i, Math.min(i + batchSize, uris.size()));

            Map<String, Object> body = Map.of("uris", batch);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            try {
                if (i == 0) {
                    // İlk batch: Playlist'i tamamen değiştir
                    restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
                } else {
                    // Sonraki batch'ler: Ekle
                    restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
                }

                Thread.sleep(100); // Rate limit koruması

            } catch (HttpClientErrorException.Unauthorized e) {
                UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
                replacePlaylistTracks(refreshed, playlistId, uris);
                return;
            } catch (Exception e) {
                System.err.println("⚠️ Failed to replace playlist tracks");
                e.printStackTrace();
            }
        }
    }

    /**
     * 🎵 Jukebox playlist'ini belirtilen cihazda çalar
     */
    public void playJukeboxPlaylist(UserInfo user, String deviceId) {
        String playlistId = user.getJukeboxPlaylistId();

        if (playlistId == null || playlistId.isEmpty()) {
            System.out.println("⚠️ No jukebox playlist, creating one...");
            playlistId = createJukeboxPlaylist(user);
        }

        playOnDevice(user, deviceId, playlistId);
        System.out.println("🎵 Jukebox playlist started on device: " + deviceId);
    }
}