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
import java.util.function.Function;
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

            // 1️⃣ Şu an çalan şarkıyı bul
            Map<String, Object> nowPlaying = getNowPlaying(user);
            String currentTrackUri = null;
            String currentTrackId = null;

            if (nowPlaying != null && nowPlaying.containsKey("item")) {
                Map<String, Object> item = (Map<String, Object>) nowPlaying.get("item");
                currentTrackId = (String) item.get("id");
                currentTrackUri = (String) item.get("uri");
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

            // 4️⃣ Şu an çalan şarkıyı en başta tut
            if (currentTrackUri != null) {
                orderedUris.remove(currentTrackUri); // varsa kaldır
                orderedUris.add(0, currentTrackUri); // başa ekle
            }

            // 3️⃣ Playlist'i güncelle
            replacePlaylistTracks(user, playlistId, orderedUris);
            System.out.println("✅ Jukebox playlist updated: " + orderedUris.size() + " tracks reordered");


        } catch (Exception e) {
            System.err.println("❌ Failed to update jukebox playlist for: " + user.getSpotifyUserId());
            e.printStackTrace();
        }
    }

    private String getActiveDeviceId(UserInfo user) {
        try {
            String url = "https://api.spotify.com/v1/me/player/devices";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + user.getAccessToken());

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> devices = response.getBody();

            if (devices == null || !devices.containsKey("devices")) {
                return null;
            }

            List<Map<String, Object>> deviceList = (List<Map<String, Object>>) devices.get("devices");
            for (Map<String, Object> device : deviceList) {
                if (device.containsKey("is_active") && (Boolean) device.get("is_active")) {
                    return (String) device.get("id");
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println("❌ Failed to get active device ID for: " + user.getSpotifyUserId());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 🔄 Playlist şarkılarını oylamaya göre sıralar
     */
    private List<String> sortPlaylistByVotes(UserInfo user, List<Map<String, Object>> tracks) {
        // Oy sayılarını ve cooldown listesini çek
        Map<String, Long> voteCounts = voteService.getActiveVotes(user.getSpotifyUserId());
        List<String> cooldownTracks = voteService.getCooldownTracks(user.getSpotifyUserId());

        // Normalize helper
        Function<String, String> normalize = ref -> {
            if (ref == null) return null;
            return ref.startsWith("spotify:track:") ? ref.substring("spotify:track:".length()) : ref;
        };

        // Şarkıları 3 kategoriye ayır
        List<Map<String, Object>> votedTracks = new ArrayList<>();
        List<Map<String, Object>> unvotedTracks = new ArrayList<>();
        List<Map<String, Object>> cooldownTracksInPlaylist = new ArrayList<>();

        for (Map<String, Object> track : tracks) {
            String trackId = (String) track.get("id");
            String uri = (String) track.get("uri");

            if (trackId == null || uri == null) continue;

            String normalizedId = normalize.apply(trackId);
            String normalizedUri = normalize.apply(uri);

            boolean isCooldown = cooldownTracks.stream()
                    .anyMatch(t -> t.equals(normalizedId) || t.equals(normalizedUri));

            Long votes = voteCounts.getOrDefault(normalizedId,
                    voteCounts.getOrDefault(normalizedUri, 0L));

            if (isCooldown) {
                cooldownTracksInPlaylist.add(track);
            } else if (votes > 0) {
                track.put("votes", votes);
                votedTracks.add(track);
            } else {
                track.put("votes", 0L);
                unvotedTracks.add(track);
            }
        }

        // Oylanmış şarkıları oy sayısına göre sırala (çok oy → öne)
        votedTracks.sort((a, b) -> Long.compare(
                (long) b.getOrDefault("votes", 0L),
                (long) a.getOrDefault("votes", 0L)
        ));

        // Final sıralama: [Oylanmış] + [Oylanmamış] + [Cooldown]
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

    /**
     * 🗳️ En çok oy alan şarkıyı queue'nun başına ekler
     */
    public void reorderQueueByVotes(UserInfo user) {
        try {
            String playlistId = user.getJukeboxPlaylistId();
            if (playlistId == null || playlistId.isEmpty()) {
                System.out.println("⏸️ No active jukebox");
                return;
            }

            // 1️⃣ Şu an çalan şarkıyı al
            Map<String, Object> nowPlaying = getNowPlaying(user);
            if (nowPlaying == null || !nowPlaying.containsKey("item")) {
                System.out.println("⚠️ Nothing playing");
                return;
            }

            String currentTrackId = (String) ((Map) nowPlaying.get("item")).get("id");

            // 2️⃣ Playlist şarkılarını çek
            List<Map<String, Object>> playlistTracks = getPlaylistTracks(user, playlistId);
            if (playlistTracks.isEmpty()) {
                return;
            }

            // 3️⃣ Oyları al
            Map<String, Long> voteCounts = voteService.getActiveVotes(user.getSpotifyUserId());
            List<String> cooldownTracks = voteService.getCooldownTracks(user.getSpotifyUserId());

            // 4️⃣ Şu an çalanı ve cooldown'dakileri çıkar
            List<Map<String, Object>> votableTracks = playlistTracks.stream()
                    .filter(track -> {
                        String trackId = (String) track.get("id");
                        return trackId != null
                                && !trackId.equals(currentTrackId)
                                && !cooldownTracks.contains(trackId);
                    })
                    .collect(Collectors.toList());

            if (votableTracks.isEmpty()) {
                System.out.println("⚠️ No votable tracks");
                return;
            }

            // 5️⃣ En çok oy alan şarkıyı bul
            Map<String, Object> topTrack = votableTracks.stream()
                    .max((a, b) -> {
                        String idA = (String) a.get("id");
                        String idB = (String) b.get("id");
                        long voteA = voteCounts.getOrDefault(idA, 0L);
                        long voteB = voteCounts.getOrDefault(idB, 0L);
                        return Long.compare(voteA, voteB);
                    })
                    .orElse(null);

            if (topTrack == null) {
                return;
            }

            String topTrackId = (String) topTrack.get("id");
            long topVotes = voteCounts.getOrDefault(topTrackId, 0L);

            // 6️⃣ Sadece oy varsa queue'ya ekle
            if (topVotes > 0) {
                addToQueue(user, topTrackId);
                System.out.println("✅ Added top voted track to queue: " + topTrack.get("name") + " (" + topVotes + " votes)");
            } else {
                System.out.println("⚠️ No votes yet");
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to reorder queue");
            e.printStackTrace();
        }
    }

    /**
     * 🎵 Queue'ya şarkı ekler
     */
    private void addToQueue(UserInfo user, String trackId) {
        String url = "https://api.spotify.com/v1/me/player/queue?uri=spotify:track:" + trackId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken());

        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            UserInfo refreshed = spotifyRefreshService.refreshAccessToken(user);
            addToQueue(refreshed, trackId);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to add to queue");
        }
    }

    /**
     * 🎵 Playlist'in şarkılarını oy bilgisiyle döndürür (Client için)
     */
    public List<Map<String, Object>> getUpcomingTracksWithVotes(UserInfo user) {
        try {
            String playlistId = user.getJukeboxPlaylistId();
            System.out.println("playlistId: " + playlistId);
            if (playlistId == null) {
                return Collections.emptyList();
            }

            // 1️⃣ Şu an çalan şarkıyı bul
            Map<String, Object> nowPlaying = getNowPlaying(user);
            String currentTrackId = null;

            if (nowPlaying != null && nowPlaying.containsKey("item")) {
                currentTrackId = (String) ((Map) nowPlaying.get("item")).get("id");
            }

            // 2️⃣ Playlist şarkılarını çek
            List<Map<String, Object>> tracks = getPlaylistTracks(user, playlistId);

            // 3️⃣ Şu an çalanı çıkar
            final String currentId = currentTrackId;
            List<Map<String, Object>> upNext = tracks.stream()
                    .filter(track -> {
                        String trackId = (String) track.get("id");
                        return trackId != null && !trackId.equals(currentId);
                    })
                    .collect(Collectors.toList());

            // 4️⃣ Oy bilgilerini ekle
            Map<String, Long> votes = voteService.getActiveVotes(user.getSpotifyUserId());
            upNext.forEach(track -> {
                String trackId = (String) track.get("id");
                track.put("votes", votes.getOrDefault(trackId, 0L));
            });

            // 5️⃣ Oylamaya göre sırala (sadece görsel için)
            upNext.sort((a, b) -> {
                long voteA = (long) a.getOrDefault("votes", 0L);
                long voteB = (long) b.getOrDefault("votes", 0L);
                return Long.compare(voteB, voteA); // Çok oy → öne
            });

            return upNext;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}