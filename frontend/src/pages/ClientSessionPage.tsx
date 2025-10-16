import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import axios from "axios";

interface Track {
  id: string;
  name: string;
  artist: string;
  albumArt: string;
  votes: number;
}

export default function ClientSessionPage() {
  const [params] = useSearchParams();
  const ownerId = params.get("ownerId");
  const [nowPlaying, setNowPlaying] = useState<Track | null>(null);
  const [upNext, setUpNext] = useState<Track[]>([]);
  const [voted, setVoted] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!ownerId) {
      setError("No session found. Please scan a valid QR code.");
      return;
    }

    const fetchNowPlaying = async () => {
      try {
        const res = await axios.get(
          `http://localhost:8080/api/spotify/now-playing/${ownerId}`
        );
        const item = res.data.item;
        if (!item) return;

        setNowPlaying({
          id: item.id,
          name: item.name,
          artist: item.artists.map((a: any) => a.name).join(", "),
          albumArt: item.album.images[0]?.url || "",
          votes: 0,
        });
      } catch (err) {
        console.error("Failed to fetch now playing:", err);
        setError("Failed to connect to Spotify session.");
      }
    };

    const fetchQueue = async () => {
      try {
        const res = await axios.get(
          `http://localhost:8080/api/spotify/queue/${ownerId}`
        );
        const queue = res.data.queue || [];
        console.log("🎶 Spotify Queue:", queue);

        const uniqueTracks = new Map<string, any>();
queue.forEach((track: any) => {
  if (track.id && !uniqueTracks.has(track.id)) {
    uniqueTracks.set(track.id, track);
  }
});

setUpNext(
  Array.from(uniqueTracks.values()).map((track: any) => ({
    id: track.id,
    name: track.name,
    artist: track.artists.map((a: any) => a.name).join(", "),
    albumArt: track.album.images[0]?.url || "",
    votes: 0,
  }))
);
      } catch (err) {
        console.error("Failed to fetch queue:", err);
      }
    };

    fetchNowPlaying();
    fetchQueue();

    // 🔄 Her 10 saniyede bir yenile
    const interval = setInterval(() => {
      fetchNowPlaying();
      fetchQueue();
    }, 10000);
    return () => clearInterval(interval);
  }, [ownerId]);

  const handleVote = (trackId: string) => {
    setVoted(trackId);
    setUpNext((prev) =>
      prev.map((t) =>
        t.id === trackId ? { ...t, votes: t.votes + 1 } : t
      )
    );
    setTimeout(() => setVoted(null), 2000);
  };

  if (error)
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#121212] text-gray-400">
        <h2 className="text-2xl text-red-400 font-bold mb-2">Session Error</h2>
        <p>{error}</p>
      </div>
    );

  if (!nowPlaying)
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#121212] text-gray-400">
        <p>Connecting to Jukebox...</p>
      </div>
    );

  return (
    <div className="min-h-screen bg-[#121212] text-gray-200 flex flex-col items-center px-4 py-8">
      {/* Now Playing */}
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-green-400 mb-4">
          Now Playing 🎵
        </h1>
        <div className="bg-[#181818] rounded-2xl shadow-lg p-6 border border-gray-800 max-w-md mx-auto">
          <img
            src={nowPlaying.albumArt}
            alt={nowPlaying.name}
            className="w-64 h-64 mx-auto rounded-xl mb-4 shadow-md"
          />
          <h2 className="text-2xl font-semibold">{nowPlaying.name}</h2>
          <p className="text-gray-400">{nowPlaying.artist}</p>
        </div>
      </div>

      {/* Up Next */}
      <div className="w-full max-w-2xl">
        <h3 className="text-xl font-semibold text-green-400 mb-4 text-center">
          Up Next
        </h3>
        {upNext.length === 0 ? (
          <p className="text-gray-500 text-center">No upcoming tracks.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            {upNext.map((track) => (
              <div
                key={track.id}
                className={`bg-[#181818] p-4 rounded-xl border ${
                  voted === track.id
                    ? "border-green-500 ring-1 ring-green-400"
                    : "border-gray-800"
                } transition-all duration-300 hover:scale-105`}
              >
                <img
                  src={track.albumArt}
                  alt={track.name}
                  className="w-full h-40 object-cover rounded-lg mb-3"
                />
                <h4 className="font-semibold">{track.name}</h4>
                <p className="text-gray-400 text-sm mb-3">{track.artist}</p>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-500">
                    {track.votes} votes
                  </span>
                  <button
                    onClick={() => handleVote(track.id)}
                    className={`px-3 py-1 rounded-full text-sm font-medium ${
                      voted === track.id
                        ? "bg-green-500 text-black"
                        : "bg-gray-700 hover:bg-green-500 hover:text-black"
                    }`}
                    disabled={voted !== null}
                  >
                    {voted === track.id ? "Voted ✅" : "Vote"}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="mt-10 text-gray-500 text-sm text-center">
        Connected to session:{" "}
        <span className="text-green-400">{ownerId}</span>
      </div>
    </div>
  );
}
