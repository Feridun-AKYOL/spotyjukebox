import { useLocation, useNavigate } from "react-router-dom";

export default function SuccessPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { selectedPlaylist, selectedDevice } = location.state || {};

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-950 text-gray-200 px-4">
      <div className="bg-gray-900 p-8 rounded-2xl border border-gray-800 text-center max-w-md">
        <h2 className="text-2xl font-bold mb-4">All Set! 🎉</h2>
        <p className="text-gray-400 mb-6">
          <span className="text-green-400 font-medium">
            {selectedPlaylist?.name}
          </span>{" "}
          will start playing on{" "}
          <span className="text-green-400 font-medium">
            {selectedDevice?.name}
          </span>
          .
        </p>
        <button
          onClick={() => navigate("/playlists")}
          className="px-5 py-2 bg-green-600 hover:bg-green-500 rounded-lg font-medium"
        >
          Back to Playlists
        </button>
      </div>
    </div>
  );
}
