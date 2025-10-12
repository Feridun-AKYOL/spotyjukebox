import { useLocation, useNavigate } from "react-router-dom";

export default function ConfirmPage() {
  const navigate = useNavigate();
  const location = useLocation();

  // 🔹 Önceki sayfalardan gelen veriler
  const { selectedPlaylist, selectedDevice } = location.state || {};

  if (!selectedPlaylist || !selectedDevice) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-950 text-gray-400 text-lg">
        Missing playlist or device information.
        <button
          className="mt-4 px-4 py-2 bg-green-600 hover:bg-green-500 rounded-lg"
          onClick={() => navigate("/playlists")}
        >
          Go Back
        </button>
      </div>
    );
  }

  const handleConfirm = () => {
    // 🔹 İleride burada jukebox başlatma veya Spotify API "play" çağrısı yapılacak
    console.log("✅ Confirmed selection:", {
      playlist: selectedPlaylist,
      device: selectedDevice,
    });

    // Şimdilik kullanıcıyı basit bir onay ekranına yönlendirebiliriz
    navigate("/success", {
      state: { selectedPlaylist, selectedDevice },
    });
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-950 text-gray-200 px-4">
      <div className="w-full max-w-xl bg-gray-900 rounded-2xl shadow-lg p-8 border border-gray-800">
        <h2 className="text-2xl font-bold text-center mb-6">
          Confirm Your Selection
        </h2>

        {/* Playlist Preview */}
        <div className="bg-gray-800 rounded-xl p-4 mb-6 flex gap-4 items-center">
          {selectedPlaylist.images?.[0] ? (
            <img
              src={selectedPlaylist.images[0].url}
              alt={selectedPlaylist.name}
              className="w-20 h-20 rounded-lg object-cover"
            />
          ) : (
            <div className="w-20 h-20 bg-gray-700 flex items-center justify-center text-gray-400 text-sm rounded-lg">
              No Image
            </div>
          )}
          <div>
            <h3 className="text-lg font-semibold text-gray-100">
              {selectedPlaylist.name}
            </h3>
            <p className="text-sm text-gray-400">
              {selectedPlaylist.tracks?.total ?? 0} tracks
            </p>
            <p className="text-xs text-gray-500 mt-1">
              by {selectedPlaylist.owner?.display_name || "Unknown"}
            </p>
          </div>
        </div>

        {/* Device Info */}
        <div className="bg-gray-800 rounded-xl p-4 mb-8">
          <p className="text-sm text-gray-400">Selected Device:</p>
          <h4 className="text-lg font-medium mt-1 text-gray-100">
            {selectedDevice.name}
          </h4>
          <p className="text-xs text-gray-500">{selectedDevice.type}</p>
        </div>

        {/* Buttons */}
        <div className="flex justify-between">
          <button
            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg"
            onClick={() => navigate(-1)}
          >
            ← Back
          </button>

          <button
            onClick={handleConfirm}
            className="px-5 py-2 bg-green-600 hover:bg-green-500 rounded-lg font-medium"
          >
            Confirm ✅
          </button>
        </div>

        {/* Step indicator */}
        <div className="mt-6 text-center text-gray-500 text-sm">
          Step 3 of 3 — <span className="text-green-400">Confirm</span>
        </div>
      </div>
    </div>
  );
}
