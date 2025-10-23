import { useContext, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { AuthContext } from "@/context/AuthProvider";

export default function SpotifyCallbackPage() {
  const navigate = useNavigate();
  const { setUser } = useContext(AuthContext);
  const hasRun = useRef(false);

  useEffect(() => {
    const linkSpotify = async () => {
      if (hasRun.current) return;
      hasRun.current = true;

      const params = new URLSearchParams(window.location.search);
      const code = params.get("code");

      if (!code ) {
        console.warn("Eksik code veya userId");
        navigate("/login");
        return;
      }

      try {
        const res = await axios.post("http://localhost:8080/api/auth/spotify/callback", {
          code,
        });

        console.log("✅ Backend kaydı:", res.data);
        const { accessToken, userId: spotifyUserId, displayName } = res.data;

        setUser({
          id: spotifyUserId ,
          displayName:displayName || "Unknown",
          access_token: accessToken, // ✅ şimdi token var
          spotifyLinked: true,
        });

        // Token URL'sini temizle (code silinsin)
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
        Spotify hesabı bağlanıyor...
      </h1>
      <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-green-600"></div>
    </div>
  );
}
