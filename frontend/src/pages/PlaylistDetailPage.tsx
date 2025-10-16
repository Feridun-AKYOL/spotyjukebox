import { useParams, useNavigate } from "react-router-dom";
import { useContext, useEffect, useState } from "react";
import { AuthContext } from "@/context/AuthProvider";
import PlaylistService from "@/services/PlaylistService";
import { Playlist } from "@/models/PlayslistModels";
import { Music, User, Clock, ArrowLeft, Play } from "lucide-react";

export default function PlaylistDetailPage() {
  const { id } = useParams();
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();
  const [playlist, setPlaylist] = useState<Playlist | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const playlistService = user?.access_token
    ? PlaylistService(user.access_token)
    : null;

  useEffect(() => {
    if (!playlistService || !id) return;
    playlistService
      .getPlaylist(id)
      .then((res) => {
        setPlaylist(res);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, [playlistService, id]);

  const formatDuration = (ms: number) => {
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, "0")}`;
  };

  const getTotalDuration = () => {
    if (!playlist?.tracks?.items) return "0:00";
    const totalMs = playlist.tracks.items.reduce(
      (acc, item) => acc + (item.track?.duration_ms || 0),
      0
    );
    const hours = Math.floor(totalMs / 3600000);
    const minutes = Math.floor((totalMs % 3600000) / 60000);
    return hours > 0 ? `${hours} hr ${minutes} min` : `${minutes} min`;
  };

  if (loading) {
    return (
      <div className="flex flex-col justify-center items-center h-screen bg-gray-950">
        <div className="animate-spin rounded-full h-16 w-16 border-t-2 border-b-2 border-green-500 mb-4"></div>
        <p className="text-gray-400 text-lg">Loading playlist details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center p-6">
        <div className="bg-red-900/30 border border-red-700 text-red-200 px-8 py-6 rounded-xl text-center max-w-md">
          <p className="font-semibold text-lg mb-2">Error loading playlist</p>
          <p className="text-sm mb-4">{error}</p>
          <button
            onClick={() => navigate(-1)}
            className="px-6 py-2 bg-red-700 hover:bg-red-600 rounded-lg transition-colors"
          >
            Go Back
          </button>
        </div>
      </div>
    );
  }

  if (!playlist) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <p className="text-gray-400">Playlist not found</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-950 via-gray-900 to-gray-950 text-gray-200">
      {/* Hero Section */}
      <div className="relative bg-gradient-to-b from-gray-800/40 to-transparent">
        <div className="max-w-7xl mx-auto px-6 py-12">
          {/* Back Button */}
          <button
            onClick={() => navigate(-1)}
            className="flex items-center gap-2 text-gray-400 hover:text-gray-200 mb-8 transition-colors group"
          >
            <ArrowLeft className="w-5 h-5 group-hover:-translate-x-1 transition-transform" />
            <span>Back to Playlists</span>
          </button>

          {/* Playlist Header */}
          <div className="flex flex-col md:flex-row gap-8 items-start md:items-end">
            {/* Playlist Cover */}
            <div className="flex-shrink-0">
              {playlist.images?.[0] ? (
                <img
                  src={playlist.images[0].url}
                  alt={playlist.name}
                  className="w-64 h-64 object-cover rounded-2xl shadow-2xl shadow-black/50"
                />
              ) : (
                <div className="w-64 h-64 bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl flex items-center justify-center shadow-2xl">
                  <Music className="w-32 h-32 text-gray-700" />
                </div>
              )}
            </div>

            {/* Playlist Info */}
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-gray-400 uppercase tracking-wide mb-3">
                Playlist
              </p>
              <h1 className="text-4xl md:text-6xl font-bold mb-6 text-gray-100 break-words">
                {playlist.name}
              </h1>
              
              {playlist.description && (
                <p className="text-gray-400 text-base mb-6 line-clamp-2">
                  {playlist.description}
                </p>
              )}

              <div className="flex flex-wrap items-center gap-2 text-sm">
                <div className="flex items-center gap-2">
                  <User className="w-4 h-4 text-gray-400" />
                  <span className="font-semibold text-gray-200">
                    {playlist.owner?.display_name || "Unknown"}
                  </span>
                </div>
                <span className="text-gray-600">•</span>
                <span className="text-gray-400">
                  {playlist.tracks?.total || 0} songs
                </span>
                <span className="text-gray-600">•</span>
                <span className="text-gray-400">{getTotalDuration()}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tracks Section */}
      <div className="max-w-7xl mx-auto px-6 py-8">
        <div className="bg-gray-900/30 backdrop-blur-sm rounded-2xl border border-gray-800 overflow-hidden">
          {/* Table Header */}
          <div className="grid grid-cols-[auto_1fr_auto] gap-4 px-6 py-4 border-b border-gray-800 text-sm font-semibold text-gray-400">
            <div className="w-12 text-center">#</div>
            <div>Title</div>
            <div className="flex items-center gap-2 justify-end">
              <Clock className="w-4 h-4" />
            </div>
          </div>

          {/* Tracks List */}
          <div className="divide-y divide-gray-800/50">
            {playlist.tracks?.items?.map((item, idx) => (
              <div
                key={idx}
                className="grid grid-cols-[auto_1fr_auto] gap-4 px-6 py-4 hover:bg-gray-800/30 transition-colors group"
              >
                {/* Track Number */}
                <div className="w-12 flex items-center justify-center">
                  <span className="text-gray-500 group-hover:hidden">
                    {idx + 1}
                  </span>
                  <Play className="w-4 h-4 text-gray-400 hidden group-hover:block" />
                </div>

                {/* Track Info */}
                <div className="min-w-0">
                  <p className="font-medium text-gray-100 truncate group-hover:text-green-400 transition-colors">
                    {item.track?.name || "Unknown Track"}
                  </p>
                  <p className="text-sm text-gray-400 truncate">
                    {item.track?.artists?.map((a) => a.name).join(", ") ||
                      "Unknown Artist"}
                  </p>
                </div>

                {/* Duration */}
                <div className="flex items-center text-sm text-gray-400">
                  {formatDuration(item.track?.duration_ms || 0)}
                </div>
              </div>
            ))}
          </div>

          {/* Empty State */}
          {(!playlist.tracks?.items || playlist.tracks.items.length === 0) && (
            <div className="text-center py-16 text-gray-500">
              <Music className="w-16 h-16 mx-auto mb-4 text-gray-700" />
              <p>No tracks in this playlist</p>
            </div>
          )}
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-8">
          <div className="bg-gray-900/50 backdrop-blur-sm border border-gray-800 rounded-xl p-6">
            <div className="flex items-center gap-3 mb-2">
              <Music className="w-5 h-5 text-green-500" />
              <span className="text-sm text-gray-400">Total Tracks</span>
            </div>
            <p className="text-3xl font-bold text-gray-100">
              {playlist.tracks?.total || 0}
            </p>
          </div>

          <div className="bg-gray-900/50 backdrop-blur-sm border border-gray-800 rounded-xl p-6">
            <div className="flex items-center gap-3 mb-2">
              <Clock className="w-5 h-5 text-blue-500" />
              <span className="text-sm text-gray-400">Duration</span>
            </div>
            <p className="text-3xl font-bold text-gray-100">
              {getTotalDuration()}
            </p>
          </div>

          <div className="bg-gray-900/50 backdrop-blur-sm border border-gray-800 rounded-xl p-6">
            <div className="flex items-center gap-3 mb-2">
              <User className="w-5 h-5 text-purple-500" />
              <span className="text-sm text-gray-400">Owner</span>
            </div>
            <p className="text-xl font-bold text-gray-100 truncate">
              {playlist.owner?.display_name || "Unknown"}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}