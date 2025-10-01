import { AuthContext } from "@/context/AuthProvider";
import { useContext, useState, useEffect } from "react";

type UserInfo = {
  id: number;
  userId: string;
  mail:string;
  displayName:string;
  accessToken: string;
  refreshToken: string;
  createdAt: string;
  updatedAt: string;
  scopes: string[];
};

export default function UserInfoPage() {
  const {user} = useContext(AuthContext);
  const [email, setEmail] = useState(""); 
  const [data, setData] = useState<UserInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  
  // Automatically fetch Spotify userId via /me
  useEffect(() => {
    if (user?.access_token) {
      fetch("https://api.spotify.com/v1/me", {
        headers: { Authorization: `Bearer ${user.access_token}` },
      })
        .then((res) => res.json())
        .then((profile) => setEmail(profile.email))
        .catch((err) => console.error("Failed to load Spotify profile", err));
    }
  }, [user]);

  const fetchUser = async () => {
    if(!email.trim()){
      setError("Please enter an email address");
      return;
    }

    setLoading(true);
    setError(null);
    setData(null);

    try {
        const url = `http://localhost:8080/user/get-by-email/${encodeURIComponent(email)}`;
      console.log("📡 Request URL:", url);
      
    const res = await fetch(url);
    
    console.log('📥 Response status:', res.status); // DEBUG
    
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || "Request failed");
      }
      const json = (await res.json()) as UserInfo;
      console.log('✅ User data received:', json); // DEBUG
      setData(json);
    } catch (e: any) {
      console.error('❌ Fetch error:', e); // DEBUG
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-2xl mx-auto bg-white rounded-xl shadow p-6">
        <h1 className="text-2xl font-bold mb-4">User Info</h1>

        <div className="flex gap-2 mb-4">
          <input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="flex-1 border rounded px-3 py-2"
            placeholder="Enter userId"
          />
          <button
            onClick={fetchUser}
            disabled={loading}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            {loading ? "Loading..." : "Fetch"}
          </button>
        </div>

        {error && (
          <div className="text-red-600 mb-4">Error: {error}</div>
        )}

        {data && (
          <div className="space-y-2">
            <p><strong>UserId:</strong> {data.userId}</p>
            <p><strong>Scopes:</strong> {data.scopes?.join(", ") || "—"}</p>
            <p><strong>Created:</strong> {data.createdAt}</p>
            <p><strong>Updated:</strong> {data.updatedAt}</p>
            <details className="mt-2">
              <summary className="cursor-pointer text-blue-600">Raw JSON</summary>
              <pre className="bg-gray-100 p-3 rounded text-sm">
                {JSON.stringify(data, null, 2)}
              </pre>
            </details>
          </div>
        )}
      </div>
    </div>
  );
}
