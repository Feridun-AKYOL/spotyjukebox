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
      if (!user?.id) {
        setIsChecking(false);
        return;
      }

      try {
        const res = await axios.get(
          `http://localhost:8080/api/auth/spotify/me/${user.id}`
        );

        if (res.status === 200 && res.data?.userId) {
          setUserExists(true);
          setUser(res.data); // kullanıcıyı context'e de yaz
        } else {
          setUserExists(false);
        }
      } catch (err) {
        console.warn("⚠️ User not found in backend:", err);
        setUserExists(false);
      } finally {
        setIsChecking(false);
      }
    };

    checkUserInBackend();
  }, [user?.id, setUser]);

  const handleSpotifyLogin = async () => {
  try {
    const res = await axios.get("http://localhost:8080/api/auth/spotify/login");
    if (res.data?.authorizeUrl) {
      console.log("🎧 Redirecting to Spotify:", res.data.authorizeUrl);
      window.location.href = res.data.authorizeUrl;
    } else {
      console.error("❌ No redirect URL received from backend:", res.data);
    }
  } catch (err) {
    console.error("❌ Spotify login failed:", err);
  }
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
            ? "You're already signed in 🎧"
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
