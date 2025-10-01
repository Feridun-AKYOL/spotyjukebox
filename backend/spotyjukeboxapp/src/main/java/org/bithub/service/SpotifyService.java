package org.bithub.service;

import org.bithub.model.SpotifyPlaylist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class SpotifyService {

    @Value("${spotify.api.url}")
    private String spotifyApiUrl;

    private final RestTemplate restTemplate;

    public SpotifyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

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
}
