import { useEffect, useState, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import axios from "axios";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

interface Track {
  id: string;
  name: string;
  artist: string;
  albumArt: string;
  votes: number;
  inCooldown?: boolean;
  cooldownRemaining?: number;
}

export default function ClientSessionPage() {
  const [params] = useSearchParams();
  const ownerId = params.get("ownerId");

  const [nowPlaying, setNowPlaying] = useState<Track | null>(null);
  const nowPlayingRef = useRef<Track | null>(null);
  const [upNext, setUpNext] = useState<Track[]>([]);
  const [votes, setVotes] = useState<Record<string, number>>({});
  const [voted, setVoted] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [voteError, setVoteError] = useState<string | null>(null);

  // Generate persistent unique client ID (stored in localStorage)
  const [clientId] = useState(() => {
    let existing = localStorage.getItem("clientId");
    if (!existing) {
      existing = "guest-" + Math.random().toString(36).substring(2, 10);
      localStorage.setItem("clientId", existing);
    }
    return existing;
  });

  // 📡 WebSocket connection for real-time vote updates
  useEffect(() => {
    if (!ownerId) return;

    const socket = new SockJS("http://localhost:8080/ws");
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        // Subscribe to session-specific topic
        client.subscribe(`/topic/votes/${ownerId}`, (message) => {
          const updated = JSON.parse(message.body);
          setVotes(updated);
        });
      },
    });

    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [ownerId]);

  // 🎵 Fetch now-playing track + queue periodically
  useEffect(() => {
    if (!ownerId) {
      setError("No session found. Please scan a valid QR code.");
      return;
    }

    const fetchNowPlaying = async () => {
      try {
        const res = await axios.get(
          `http://127.0.0.1:8080/api/spotify/now-playing/${ownerId}`
        );
        const item = res.data.item;
        if (!item) return;

        const currentTrackId = nowPlayingRef.current?.id;
        const currentTrackName = nowPlayingRef.current?.name;

        // Detect song change (backend reset + local cleanup)
        if (currentTrackId && currentTrackId !== item.id) {
          console.log(`🎵 Track changed from ${currentTrackName} to ${item.name}`);

          // Inform backend to reset votes for previous track
          try {
            await axios.post("http://127.0.0.1:8080/api/jukebox/played", {
              ownerId,
              trackId: currentTrackId,
            });
            console.log("✅ Backend reset votes for:", currentTrackId);
          } catch (err) {
            console.warn("⚠️ Failed to reset votes:", err);
          }

          // Remove finished track’s votes locally
          setVotes((prev) => {
            const newVotes = { ...prev };
            delete newVotes[currentTrackId];
            return newVotes;
          });

          // Remove finished track from queue
          setUpNext((prev) => prev.filter((t) => t.id !== currentTrackId));
        }

        // Update current track (state + ref)
        const newTrack = {
          id: item.id,
          name: item.name,
          artist: item.artists.map((a: any) => a.name).join(", "),
          albumArt: item.album.images[0]?.url || "",
          votes: 0,
        };

        setNowPlaying(newTrack);
        nowPlayingRef.current = newTrack;
      } catch (err) {
        console.error("Failed to fetch now playing:", err);
        setError("Failed to connect to Spotify session.");
      }
    };

    const fetchQueue = async () => {
      try {
        // Retrieve playlist with vote & cooldown info
        const res = await axios.get(
          `http://localhost:8080/api/spotify/upcoming-tracks/${ownerId}`
        );
        const queue = res.data.queue || [];

        setUpNext(
          queue.map((track: any) => ({
            id: track.id,
            name: track.name,
            artist: track.artists?.map((a: any) => a.name).join(", ") || "Unknown",
            albumArt: track.album?.images?.[0]?.url || "",
            votes: track.votes || 0,
            inCooldown: track.inCooldown || false,
            cooldownRemaining: track.cooldownRemaining || 0,
          }))
        );
      } catch (err) {
        console.error("Failed to fetch queue:", err);
      }
    };

    fetchNowPlaying();
    fetchQueue();

    // Refresh data every 10 seconds
    const interval = setInterval(() => {
      fetchNowPlaying();
      fetchQueue();
    }, 10000);

    return () => clearInterval(interval);
  }, [ownerId]);

  // 🗳 Send vote request to backend
  const handleVote = async (trackId: string) => {
    if (!ownerId) return;
    setVoted(trackId);

    try {
      await axios.post("http://localhost:8080/api/jukebox/vote", {
        ownerId,
        trackId,
        clientId,
      });
    } catch (err: any) {
      const message =
        err.response?.data?.error ||
        err.response?.data?.message ||
        "Vote failed";
      console.warn("Vote error:", message);

      // Handle duplicate or failed votes
      if (message.includes("already voted")) {
        setVoteError("⚠️ You already voted for this song.");
      } else {
        setVoteError("❌ Vote failed. Try again.");
      }
    } finally {
      setTimeout(() => setVoted(null), 2000);
    }
  };

  // Auto-clear vote error after a few seconds
  useEffect(() => {
    if (voteError) {
      const timer = setTimeout(() => setVoteError(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [voteError]);

  // 🎧 Show loading screen before data arrives
  if (!nowPlaying)
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#121212] text-gray-400">
        <p>Connecting to Jukebox...</p>
      </div>
    );

  // 🖼️ UI render
  return (
    <div className="min-h-screen bg-[#121212] text-gray-200 flex flex-col items-center px-4 py-8">
      {/* ⚠️ Error Banner */}
      {voteError && (
        <div
          className="fixed top-4 left-1/2 transform -translate-x-1/2
                     bg-yellow-500/20 border border-yellow-400 
                     text-yellow-300 text-sm px-4 py-2 rounded-lg 
                     text-center shadow-lg backdrop-blur-md 
                     animate-pulse transition-opacity duration-500 
                     z-50"
        >
          {voteError} <br /> {error}
        </div>
      )}

      <div className="text-center mb-10">
        {/* NOW PLAYING SECTION */}
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

      {/* UP NEXT SECTION */}
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
                className={`bg-[#181818] p-4 rounded-xl border transition-all duration-300 ${
                  track.inCooldown
                    ? "opacity-60 border-gray-700"
                    : voted === track.id
                    ? "border-green-500 ring-1 ring-green-400 hover:scale-105"
                    : "border-gray-800 hover:scale-105"
                }`}
              >
                <div className="relative">
                  <img
                    src={track.albumArt}
                    alt={track.name}
                    className={`w-full h-40 object-cover rounded-lg mb-3 ${
                      track.inCooldown ? "grayscale" : ""
                    }`}
                  />
                  {/* Cooldown badge for recently played tracks */}
                  {track.inCooldown && (
                    <div className="absolute top-2 right-2 bg-orange-500/90 text-xs font-semibold px-2 py-1 rounded-full text-white backdrop-blur-sm">
                      🕐 Cooldown
                    </div>
                  )}
                </div>
                <h4 className="font-semibold">{track.name}</h4>
                <p className="text-gray-400 text-sm mb-3">{track.artist}</p>

                {track.inCooldown ? (
                  // Display cooldown info (instead of vote button)
                  <div className="text-center py-2 bg-gray-800/50 rounded-lg">
                    <p className="text-xs text-orange-400 font-medium">
                      Recently played
                    </p>
                    <p className="text-xs text-gray-500 mt-1">
                      Available in {track.cooldownRemaining} song
                      {track.cooldownRemaining !== 1 ? "s" : ""}
                    </p>
                  </div>
                ) : (
                  // Vote button for active tracks
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      {votes[track.id] ?? track.votes ?? 0} votes
                    </span>
                    <button
                      onClick={() => handleVote(track.id)}
                      className={`px-3 py-1 rounded-full text-sm font-medium transition ${
                        voted === track.id
                          ? "bg-green-500 text-black cursor-not-allowed"
                          : "bg-gray-700 hover:bg-green-500 hover:text-black"
                      }`}
                      disabled={voted !== null}
                    >
                      {voted === track.id ? "Voted ✅" : "Vote"}
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* FOOTER */}
      <div className="mt-10 text-gray-500 text-sm text-center">
        Connected to session:{" "}
        <span className="text-green-400 font-mono">{ownerId}</span>
      </div>
    </div>
  );
}
