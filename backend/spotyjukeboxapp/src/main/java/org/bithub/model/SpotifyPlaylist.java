package org.bithub.model;

import lombok.Data;

/**
 * Represents a Spotify playlist associated with a user.
 * Contains basic metadata used for displaying and managing playlists.
 */
@Data
public class SpotifyPlaylist {

    /** The unique Spotify playlist ID. */
    private String id;

    /** The display name of the playlist. */
    private String name;

    /** The URL of the playlist's cover image. */
    private String imageUrl;

    /** The total number of tracks in the playlist. */
    private int trackCount;

    /** The Spotify API endpoint URL for this playlist. */
    private String href;
}
