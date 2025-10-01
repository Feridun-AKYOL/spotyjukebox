import { useContext, useEffect, useState } from "react";
import { AuthContext } from "@/context/AuthProvider";
import PlaylistService from "@/services/PlaylistService";
import { Playlist } from "@/models/PlayslistModels";

export default function PlaylistPage() {
  const { user } = useContext(AuthContext);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  // Create Playlist service if token exists
  const playlistService = user?.access_token
    ? PlaylistService(user.access_token)
    : null;

  useEffect(() => {
    if (!playlistService) return;
    playlistService
      .getUserPlaylists(20, 0)
      .then((res) => setPlaylists(res.items))
      .catch((err) => setError(err.message));
  }, [playlistService]);

  const handleSelect = (id: string) => {
    setSelected(id);
    console.log("Selected playlist:", id);
    // ileride: backend’e gönder → jukebox base playlist olarak kaydet
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-indigo-50 py-10 px-4">
      <div className="max-w-5xl mx-auto">
        <h1 className="text-3xl font-extrabold mb-6 text-gray-800">
          Your Playlists
        </h1>

        {error && (
          <div className="bg-red-100 text-red-700 p-4 rounded mb-4">
            Error: {error}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
          {playlists.map((playlist) => (
            <div
              key={playlist.id}
              className={`cursor-pointer rounded-xl shadow-md overflow-hidden border-2 transition ${
                selected === playlist.id
                  ? "border-indigo-500 ring-2 ring-indigo-300"
                  : "border-transparent"
              }`}
              onClick={() => handleSelect(playlist.id)}
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
                  {playlist.tracks.total} tracks
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  by {playlist.owner.display_name}
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

        {selected && (
          <div className="mt-8 text-center">
            <p className="text-indigo-700 font-medium">
              Selected Playlist ID: {selected}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
