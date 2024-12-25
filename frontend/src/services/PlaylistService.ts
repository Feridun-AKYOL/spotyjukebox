import { Playlist, SpotifyApiResponse } from "@/models/PlayslistModels";
import { createSpotifyClient } from "./spotifyClientBase";


const PlaylistService = (token: string) => {
    const baseClient = createSpotifyClient(token);
    return {
        /**
        * Search for items in Spotify.
        * @param query The search query string.
        * @param type The type of items to search for (e.g., "track", "artist").
        * @param limit The number of items to return.
        * @returns The search results.
        */
        search: (query: string, type: string, limit = 20) => {
            const params = new URLSearchParams({ q: query, type, limit: limit.toString() });
            baseClient(`/search?${params.toString()}`);
        },
        /**
         * Get a user's playlists.
         * @param limit The maximum number of playlists to return.
         * @param offset The index of the first playlist to return.
         * @returns The user's playlists.
         */
        getUserPlaylists: (limit = 20, offset = 0) : Promise<SpotifyApiResponse<Playlist>> => {
            const params = new URLSearchParams({ limit: limit.toString(), offset: offset.toString() });
            return baseClient(`/me/playlists?${params.toString()}`);
        },
        getPlaylist: (playlistId: string) => baseClient(`/playlists/${playlistId}`),

        /**
         * Add tracks to a playlist.
         * @param playlistId The ID of the playlist.
         * @param uris An array of track URIs to add.
         * @returns The response from Spotify.
         */
        addTracksToPlaylist: (playlistId: string, uris: string[]) => {
            return baseClient(`/playlists/${playlistId}/tracks`, {
                method: "POST",
                body: { uris },
            });
        },

        /**
         * Get an artist's details.
         * @param artistId The ID of the artist.
         * @returns The artist's details.
         */
        getArtist: (artistId: string) => baseClient(`/artists/${artistId}`),

        /**
         * Get top tracks for an artist.
         * @param artistId The ID of the artist.
         * @param market The market code (e.g., "US").
         * @returns The artist's top tracks.
         */
        getArtistTopTracks: (artistId: string, market = "US") => {
            return baseClient(`/artists/${artistId}/top-tracks?market=${market}`);
        },
    };
};

export default PlaylistService;
