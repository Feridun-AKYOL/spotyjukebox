import { useContext, useEffect, useState } from "react";
import { AuthContext } from "@/context/AuthProvider";
import axios from "axios";
import { Playlist } from "@/models/PlayslistModels";
import { useNavigate } from "react-router-dom";

export default function PlaylistPage() {
  const { user } = useContext(AuthContext);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!user?.id) return;

    axios
      .get(`http://localhost:8080/api/spotify/playlists/${user.id}`)
      .then((res) => {
        console.log("🎵 Spotify playlists:", res.data);
        setPlaylists(res.data.items || []); // ✅ fix: always array
      })
      .catch((err) => {
        console.error("❌ Playlist fetch error:", err);
        setError("Could not load playlists");
      })
      .finally(() => setLoading(false));
  }, [user?.id]);

  const handleNext = () => {
    if (selected) {
      const chosen = playlists.find((p) => p.id === selected);
      navigate("/devices", { state: { selectedPlaylist: chosen } });
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen text-gray-400 text-lg">
        Loading playlists...
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-950 text-gray-200 px-4">
      <div className="w-full max-w-6xl bg-gray-900 rounded-2xl shadow-lg p-8 border border-gray-800">
        <h1 className="text-3xl font-bold mb-6 text-center">
          Select a Playlist
        </h1>

        {error && (
          <div className="bg-red-900 text-red-200 px-4 py-2 rounded-md mb-4 text-center">
            {error}
          </div>
        )}

        {/* Playlist Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
          {playlists.map((playlist) => (
            <div
              key={playlist.id}
              onClick={() => setSelected(playlist.id)}
              className={`cursor-pointer rounded-xl overflow-hidden border-2 transition-all duration-200 ${
                selected === playlist.id
                  ? "border-green-500 ring-2 ring-green-400"
                  : "border-gray-800 hover:border-gray-600"
              }`}
            >
              {playlist.images?.[0] ? (
                <img
                  src={playlist.images[0].url}
                  alt={playlist.name}
                  className="w-full h-48 object-cover"
                />
              ) : (
                <div className="w-full h-48 bg-gray-800 flex items-center justify-center text-gray-500">
                  No Image
                </div>
              )}
              <div className="p-4 bg-gray-900">
                <h2 className="font-semibold text-lg text-gray-100 truncate">
                  {playlist.name}
                </h2>
                <p className="text-sm text-gray-400">
                  {playlist.tracks?.total ?? 0} tracks
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  by {playlist.owner?.display_name || "Unknown"}
                </p>
              </div>
            </div>
          ))}
        </div>

        {/* Empty state */}
        {(!playlists || playlists.length === 0) && !error && (
          <div className="text-center text-gray-500 mt-12">
            No playlists found.
          </div>
        )}

        {/* Navigation buttons */}
        <div className="flex justify-between mt-8">
          <button
            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg"
            onClick={() => navigate(-1)}
          >
            ← Back
          </button>
          <button
            className={`px-5 py-2 rounded-lg font-medium ${
              selected
                ? "bg-green-600 hover:bg-green-500"
                : "bg-gray-600 cursor-not-allowed"
            }`}
            onClick={handleNext}
            disabled={!selected}
          >
            Next →
          </button>
        </div>

        {/* Step indicator */}
        <div className="mt-6 text-center text-gray-500 text-sm">
          Step 1 of 3 — <span className="text-green-400">Select Playlist</span>
        </div>
      </div>
    </div>
  );
}
