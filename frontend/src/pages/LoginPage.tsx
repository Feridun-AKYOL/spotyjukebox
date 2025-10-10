import { useContext } from "react";
import { AuthContext } from "@/context/AuthProvider";
import { LogIn } from "lucide-react";

const LoginPage = () => {
  const { user } = useContext(AuthContext);

  const handleSpotifyLogin = () => {
    const clientId = import.meta.env.VITE_SPOTIFY_CLIENT_ID;
    const redirectUri = import.meta.env.VITE_SPOTIFY_REDIRECT_URI;
    const scopes = "user-read-email user-read-private playlist-read-private";

    const userId = "owner001"; 
  localStorage.setItem("userId", userId); // SpotifyCallbackPage bunu okuyacak

    const spotifyAuthUrl = `https://accounts.spotify.com/authorize?client_id=${clientId}&response_type=code&redirect_uri=${encodeURIComponent(
      redirectUri
    )}&scope=${encodeURIComponent(scopes)}&show_dialog=true`;

    console.log("🎧 Redirecting to Spotify:", spotifyAuthUrl);
    window.location.href = spotifyAuthUrl;
  };

  console.log("Client ID:", import.meta.env.VITE_SPOTIFY_CLIENT_ID);
console.log("Redirect URI:", import.meta.env.VITE_SPOTIFY_REDIRECT_URI);


  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50 flex flex-col items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-lg p-8 max-w-md w-full mx-auto text-center">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">Welcome</h1>
        <p className="text-gray-600 mb-8">
          {user ? "You're already signed in." : "Please sign in to continue"}
        </p>

        {!user && (
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
