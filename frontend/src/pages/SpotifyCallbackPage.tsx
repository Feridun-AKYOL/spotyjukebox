import { useContext, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { AuthContext } from "@/context/AuthProvider";

export default function SpotifyCallbackPage() {
  const navigate = useNavigate();
  const { setUser } = useContext(AuthContext);
  const hasRun = useRef(false); // Prevents useEffect from running twice (React strict mode)

  useEffect(() => {
    const linkSpotify = async () => {
      if (hasRun.current) return; // Avoid duplicate API calls
      hasRun.current = true;

      const params = new URLSearchParams(window.location.search);
      const code = params.get("code");

      // If missing Spotify authorization code, redirect back to login
      if (!code) {
        console.warn("Missing authorization code");
        navigate("/login");
        return;
      }

      try {
        // Exchange authorization code for access token
        const res = await axios.post("http://localhost:8080/api/auth/spotify/callback", {
          code,
        });

        console.log("✅ Backend response:", res.data);
        const { accessToken, userId: spotifyUserId, displayName } = res.data;

        // Store authenticated user in global context
        setUser({
          id: spotifyUserId,
          displayName: displayName || "Unknown",
          access_token: accessToken,
          spotifyLinked: true,
        });

        // Clean URL (remove code param) and redirect to playlists
        window.history.replaceState({}, document.title, "/playlists");
        navigate("/playlists");
      } catch (err) {
        console.error("❌ Spotify callback error:", err);
        navigate("/main");
      }
    };

    linkSpotify();
  }, [navigate, setUser]);

  return (
    <div className="min-h-screen flex flex-col justify-center items-center bg-gradient-to-br from-green-50 to-emerald-100">
      <h1 className="text-xl font-semibold text-gray-700 mb-4">
        Linking your Spotify account...
      </h1>
      <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-green-600"></div>
    </div>
  );
}
