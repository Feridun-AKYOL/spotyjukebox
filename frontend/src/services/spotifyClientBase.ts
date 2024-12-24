const BASE_URL = "https://api.spotify.com/v1";

type FetchOptions = Omit<RequestInit, "body"> & { body?: Record<string, any> | null };

/**
 * Creates a Spotify API client with a functional approach.
 * @param accessToken The Spotify API access token.
 * @returns A collection of Spotify API methods.
 */
export const createSpotifyClient = (accessToken: string) => {
    
  /**
   * Perform a fetch request with authentication and optional body.
   * @param endpoint The API endpoint (relative to base URL).
   * @param options Additional options for the request.
   * @returns The parsed JSON response.
   */
  const fetchSpotify = async (endpoint: string, options: FetchOptions = {}) => {
    const { body, ...restOptions } = options;
    const headers: HeadersInit = {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    };

    const response = await fetch(`${BASE_URL}${endpoint}`, {
      ...restOptions,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(`Spotify API error: ${error.error?.message || response.statusText}`);
    }

    return response.json();
  };

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
      return fetchSpotify(`/search?${params.toString()}`);
    },

    /**
     * Get the current user's profile.
     * @returns The user profile data.
     */
    getCurrentUser: () => fetchSpotify("/me"),

    /**
     * Get the user's playlists.
     * @param limit The number of playlists to retrieve.
     * @param offset The offset for pagination.
     * @returns The list of playlists.
     */
    getUserPlaylists: (limit = 20, offset = 0) => {
      const params = new URLSearchParams({ limit: limit.toString(), offset: offset.toString() });
      return fetchSpotify(`/me/playlists?${params.toString()}`);
    },

    /**
     * Get details of a specific playlist.
     * @param playlistId The ID of the playlist.
     * @returns The playlist details.
     */
    getPlaylist: (playlistId: string) => fetchSpotify(`/playlists/${playlistId}`),

    /**
     * Add tracks to a playlist.
     * @param playlistId The ID of the playlist.
     * @param uris An array of track URIs to add.
     * @returns The response from Spotify.
     */
    addTracksToPlaylist: (playlistId: string, uris: string[]) => {
      return fetchSpotify(`/playlists/${playlistId}/tracks`, {
        method: "POST",
        body: { uris },
      });
    },

    /**
     * Get an artist's details.
     * @param artistId The ID of the artist.
     * @returns The artist's details.
     */
    getArtist: (artistId: string) => fetchSpotify(`/artists/${artistId}`),

    /**
     * Get top tracks for an artist.
     * @param artistId The ID of the artist.
     * @param market The market code (e.g., "US").
     * @returns The artist's top tracks.
     */
    getArtistTopTracks: (artistId: string, market = "US") => {
      return fetchSpotify(`/artists/${artistId}/top-tracks?market=${market}`);
    },
  };
};
