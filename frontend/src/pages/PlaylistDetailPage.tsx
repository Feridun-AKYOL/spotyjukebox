import { useParams } from "react-router-dom";
import { useContext, useEffect, useState } from "react";
import { AuthContext } from "@/context/AuthProvider";
import PlaylistService from "@/services/PlaylistService";
import { Playlist } from "@/models/PlayslistModels";

export default function PlaylistDetailPage() {
  const { id } = useParams(); // playlist id from URL
  const { user } = useContext(AuthContext);
  const [playlist, setPlaylist] = useState<Playlist | null>(null);
  const [error, setError] = useState<string | null>(null);

  const playlistService = user?.access_token
    ? PlaylistService(user.access_token)
    : null;

  useEffect(() => {
    if (!playlistService || !id) return;
    playlistService
      .getPlaylist(id)
      .then((res) => setPlaylist(res))
      .catch((err) => setError(err.message));
  }, [playlistService, id]);

  if (error) {
    return (
      <div className="p-6 text-red-600">
        Error loading playlist: {error}
      </div>
    );
  }

  if (!playlist) {
    return <div className="p-6">Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-50 py-10 px-4">
      <div className="max-w-4xl mx-auto bg-white rounded-xl shadow-lg p-6">
        {/* Header */}
        <div className="flex gap-6 mb-6">
          {playlist.images?.[0] && (
            <img
              src={playlist.images[0].url}
              alt={playlist.name}
              className="w-48 h-48 object-cover rounded-lg shadow"
            />
          )}
          <div>
            <h1 className="text-3xl font-bold">{playlist.name}</h1>
            <p className="text-gray-600">{playlist.description}</p>
            <p className="text-sm text-gray-500 mt-2">
              by {playlist.owner.display_name} · {playlist.tracks.total} tracks
            </p>
          </div>
        </div>

        Tracks
        <div className="divide-y divide-gray-200">
          {playlist.tracks.items.map((item, idx) => (
            <div
              key={idx}
              className="flex items-center justify-between py-3"
            >
              <div>
                <p className="font-medium text-gray-800">
                  {item.track.name}
                </p>
                <p className="text-sm text-gray-500">
                  {item.track.artists.map((a) => a.name).join(", ")}
                </p>
              </div>
              <span className="text-sm text-gray-400">
                {Math.floor(item.track.duration_ms / 60000)}:
                {String(Math.floor((item.track.duration_ms % 60000) / 1000)).padStart(2, "0")}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
