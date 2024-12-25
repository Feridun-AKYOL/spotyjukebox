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
  return fetchSpotify;
};
