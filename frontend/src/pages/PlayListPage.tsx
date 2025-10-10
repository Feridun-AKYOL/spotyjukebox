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
  const navigate = useNavigate();

  useEffect(() => {
    if (!user?.id) return;

    axios
      .get(`http://localhost:8080/api/playlists/${user.id}`)
      .then((res) => {
        console.log("🎵 Spotify playlists:", res.data);
        setPlaylists(res.data.items || []);
      })
      .catch((err) => {
        console.error("❌ Playlist fetch error:", err);
        setError("Could not load playlists");
      });
  }, [user?.id]);

  const handleSelect = (id: string) => {
    setSelected(id);
    navigate(`/playlist/${id}`);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-indigo-50 py-10 px-4">
      <div className="max-w-5xl mx-auto">
        <h1 className="text-3xl font-extrabold mb-6 text-gray-800">
          Your Playlists
        </h1>

        {error && (
          <div className="bg-red-100 text-red-700 p-4 rounded mb-4">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
          {playlists.map((playlist) => (
            <div
              key={playlist.id}
              onClick={() => handleSelect(playlist.id)}
              className={`cursor-pointer rounded-xl shadow-md overflow-hidden border-2 transition ${
                selected === playlist.id
                  ? "border-indigo-500 ring-2 ring-indigo-300"
                  : "border-transparent"
              }`}
            >
              {playlist.images?.[0] ? (
                <img
                  src={playlist.images[0].url}
                  alt={playlist.name}
                  className="w-full h-48 object-cover"
                />
              ) : (
                <div className="w-full h-48 bg-gray-200 flex items-center justify-center text-gray-400">
                  No Image
                </div>
              )}
              <div className="p-4 bg-white">
                <h2 className="font-semibold text-lg text-gray-800">
                  {playlist.name}
                </h2>
                <p className="text-sm text-gray-500">
                  {playlist.tracks?.total} tracks
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  by {playlist.owner?.display_name}
                </p>
              </div>
            </div>
          ))}
        </div>

        {(!playlists || playlists.length === 0) && !error && (
          <div className="text-center text-gray-500 mt-12">
            No playlists found.
          </div>
        )}
      </div>
    </div>
  );
}
