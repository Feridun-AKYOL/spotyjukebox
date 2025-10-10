import { useContext, useEffect, useState } from "react";
import { AuthContext } from "@/context/AuthProvider";
import { LogIn } from "lucide-react";
import axios from "axios";

const LoginPage = () => {
  const { user, setUser } = useContext(AuthContext);
  const [isChecking, setIsChecking] = useState(true);
  const [userExists, setUserExists] = useState(false);

  useEffect(() => {
    const checkUserInBackend = async () => {
      try {
        // Eğer context'te kullanıcı varsa backend'de de var mı kontrol et
        if (user?.id) {
          const res = await axios.get(`http://localhost:8080/api/auth/spotify/me/${user.id}`);
          if (res.data.exists) {
            setUserExists(true);
          }
        }
      } catch {
        setUserExists(false);
      } finally {
        setIsChecking(false);
      }
    };

    checkUserInBackend();
  }, [user?.id]);

  const handleSpotifyLogin = () => {
    const clientId = import.meta.env.VITE_SPOTIFY_CLIENT_ID;
    const redirectUri = import.meta.env.VITE_SPOTIFY_REDIRECT_URI;
    const scopes = "user-read-email user-read-private playlist-read-private";

    const spotifyAuthUrl = `https://accounts.spotify.com/authorize?client_id=${clientId}&response_type=code&redirect_uri=${encodeURIComponent(
      redirectUri
    )}&scope=${encodeURIComponent(scopes)}&show_dialog=true`;

    console.log("🎧 Redirecting to Spotify:", spotifyAuthUrl);
    window.location.href = spotifyAuthUrl;
  };

  if (isChecking) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-500">Checking account...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50 flex flex-col items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-lg p-8 max-w-md w-full mx-auto text-center">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">Welcome</h1>
        <p className="text-gray-600 mb-8">
          {userExists
            ? "You're already signed in."
            : "Please sign in with your Spotify account"}
        </p>

        {!userExists && (
          <button
            onClick={handleSpotifyLogin}
            className="flex items-center justify-center gap-2 w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-3 px-6 rounded-lg transition-colors"
          >
            <LogIn size={20} />
            Sign in with Spotify
          </button>
        )}
      </div>
    </div>
  );
};

export default LoginPage;
