import { useContext, useEffect, useState } from "react";
import { AuthContext } from "@/context/AuthProvider";
import axios from "axios";
import { Playlist } from "@/models/PlayslistModels";
import { useNavigate } from "react-router-dom";
import { Music, User, PlayCircle, Info } from "lucide-react";

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
        setPlaylists(res.data.items || []);
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

  const handleViewDetails = (playlistId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    navigate(`/playlist/${playlistId}`);
  };

  if (loading) {
    return (
      <div className="flex flex-col justify-center items-center h-screen bg-gray-950">
        <div className="animate-spin rounded-full h-16 w-16 border-t-2 border-b-2 border-green-500 mb-4"></div>
        <p className="text-gray-400 text-lg">Loading your playlists...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-950 via-gray-900 to-gray-950 text-gray-200 px-4 py-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8 text-center">
          <div className="flex items-center justify-center gap-3 mb-3">
            <Music className="w-10 h-10 text-green-500" />
            <h1 className="text-4xl font-bold bg-gradient-to-r from-green-400 to-emerald-500 bg-clip-text text-transparent">
              Your Playlists
            </h1>
          </div>
          <p className="text-gray-400 text-lg">
            Select a playlist to continue or view details
          </p>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-900/30 border border-red-700 text-red-200 px-6 py-4 rounded-xl mb-6 text-center backdrop-blur-sm">
            <p className="font-medium">{error}</p>
          </div>
        )}

        {/* Playlist Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 mb-8">
          {playlists.map((playlist) => (
            <div
              key={playlist.id}
              className={`group relative rounded-2xl overflow-hidden transition-all duration-300 transform hover:scale-105 ${
                selected === playlist.id
                  ? "ring-4 ring-green-500 shadow-2xl shadow-green-500/30"
                  : "hover:shadow-xl hover:shadow-gray-900/50"
              }`}
            >
              {/* Card Container */}
              <div
                onClick={() => setSelected(playlist.id)}
                className="cursor-pointer bg-gray-900/50 backdrop-blur-sm border border-gray-800 rounded-2xl overflow-hidden h-full flex flex-col"
              >
                {/* Image Container */}
                <div className="relative aspect-square overflow-hidden">
                  {playlist.images?.[0] ? (
                    <>
                      <img
                        src={playlist.images[0].url}
                        alt={playlist.name}
                        className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-gray-900 via-transparent to-transparent opacity-60"></div>
                    </>
                  ) : (
                    <div className="w-full h-full bg-gradient-to-br from-gray-800 to-gray-900 flex items-center justify-center">
                      <Music className="w-20 h-20 text-gray-700" />
                    </div>
                  )}
                  
                  {/* Selection Indicator */}
                  {selected === playlist.id && (
                    <div className="absolute top-4 right-4 bg-green-500 rounded-full p-2 shadow-lg animate-pulse">
                      <PlayCircle className="w-6 h-6 text-white" />
                    </div>
                  )}
                </div>

                {/* Info Section */}
                <div className="p-5 flex-1 flex flex-col">
                  <h2 className="font-bold text-lg text-gray-100 mb-2 line-clamp-2 group-hover:text-green-400 transition-colors">
                    {playlist.name}
                  </h2>
                  
                  <div className="flex items-center gap-2 text-sm text-gray-400 mb-3">
                    <Music className="w-4 h-4" />
                    <span>{playlist.tracks?.total ?? 0} tracks</span>
                  </div>

                  <div className="flex items-center gap-2 text-xs text-gray-500 mb-4">
                    <User className="w-3 h-3" />
                    <span className="truncate">
                      {playlist.owner?.display_name || "Unknown"}
                    </span>
                  </div>

                  {/* View Details Button */}
                  <button
                    onClick={(e) => handleViewDetails(playlist.id, e)}
                    className="mt-auto w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-gray-800/50 hover:bg-gray-700 border border-gray-700 hover:border-gray-600 rounded-lg transition-all duration-200 text-sm font-medium group/btn"
                  >
                    <Info className="w-4 h-4 group-hover/btn:text-green-400 transition-colors" />
                    <span className="group-hover/btn:text-green-400 transition-colors">
                      View Details
                    </span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Empty State */}
        {(!playlists || playlists.length === 0) && !error && (
          <div className="text-center py-20">
            <Music className="w-24 h-24 text-gray-700 mx-auto mb-6" />
            <h3 className="text-2xl font-semibold text-gray-400 mb-2">
              No playlists found
            </h3>
            <p className="text-gray-500">
              Create some playlists on Spotify to get started!
            </p>
          </div>
        )}

        {/* Navigation Footer */}
        <div className="sticky bottom-0 bg-gray-900/80 backdrop-blur-lg border-t border-gray-800 rounded-2xl p-6 mt-8">
          <div className="flex items-center justify-between max-w-7xl mx-auto">
            <button
              className="flex items-center gap-2 px-6 py-3 bg-gray-800 hover:bg-gray-700 rounded-xl transition-all duration-200 font-medium"
              onClick={() => navigate(-1)}
            >
              <span>←</span>
              <span>Back</span>
            </button>

            <div className="flex items-center gap-6">
              {/* Step Indicator */}
              <div className="hidden sm:flex items-center gap-3 text-sm">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-green-500 flex items-center justify-center font-bold">
                    1
                  </div>
                  <span className="text-green-400 font-medium">Playlist</span>
                </div>
                <div className="w-12 h-0.5 bg-gray-700"></div>
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-gray-700 flex items-center justify-center">
                    2
                  </div>
                  <span className="text-gray-500">Device</span>
                </div>
                <div className="w-12 h-0.5 bg-gray-700"></div>
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-gray-700 flex items-center justify-center">
                    3
                  </div>
                  <span className="text-gray-500">Play</span>
                </div>
              </div>

              <button
                className={`flex items-center gap-2 px-8 py-3 rounded-xl font-semibold transition-all duration-200 ${
                  selected
                    ? "bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-500 hover:to-emerald-500 shadow-lg shadow-green-500/30"
                    : "bg-gray-700 cursor-not-allowed opacity-50"
                }`}
                onClick={handleNext}
                disabled={!selected}
              >
                <span>Continue to Devices</span>
                <span>→</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}