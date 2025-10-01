const BASE_URL = "https://api.spotify.com/v1";

type FetchOptions = Omit<RequestInit, "body"> & { body?: Record<string, any> | null };

export const createSpotifyClient = (accessToken: string) => {
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

    // special handling: rate limiting (429)
    if (response.status === 429) {
      const retryAfter = parseInt(response.headers.get("Retry-After") || "1", 10);
      console.warn(
        `Spotify API rate limit hit. Retrying after ${retryAfter} seconds...`
      );
      await new Promise((res) => setTimeout(res, retryAfter * 1000));
      return fetchSpotify(endpoint, options); // retry once
    }

    // ✔️ always read the text first
    const raw = await response.text();

    if (!response.ok) {
      let errorMessage: string;
      try {
        const errJson = JSON.parse(raw);
        errorMessage = errJson.error?.message || JSON.stringify(errJson);
      } catch {
        errorMessage = raw || response.statusText;
      }
      throw new Error(`Spotify API error (${response.status}): ${errorMessage}`);
    }

    // ✔️ if succeed
    if (response.status === 204 ||!raw) return null; // empty body
    try {
      return JSON.parse(raw);
    } catch {
      return raw; // if not json
    }
  };

  return fetchSpotify;
};
