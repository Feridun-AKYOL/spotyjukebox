import { useContext, useEffect, useState } from 'react';
import { LogOut } from 'lucide-react';
import { AuthContext } from '@/context/AuthProvider';
import PlaylistService from '@/services/PlaylistService';
import { Playlist } from '@/models/PlayslistModels';

const PostLoginPage = () => {
  const { user, signoutCallback } = useContext(AuthContext);

  const playlistService = user?.access_token
    ? PlaylistService(user.access_token)
    : null;

  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [spotifyProfile, setSpotifyProfile] = useState<any>(null);
  const [registerStatus, setRegisterStatus] = useState<'success' | 'failed' | null>(null);

  useEffect(() => {
    if (!playlistService) return;

    playlistService
      .getUserPlaylists()
      .then((data) => setPlaylists(data.items))
      .catch((err) => {
        console.error(err);
        setError(err.message);
      });
  }, [playlistService]);

  useEffect(() => {
    if (!user?.access_token || !user?.refresh_token) {
      console.warn('⚠️ Missing access_token or refresh_token');
      return;
    }

    console.log('🎵 Fetching Spotify profile...');

    fetch('https://api.spotify.com/v1/me', {
      headers: {
        Authorization: `Bearer ${user.access_token}`,
      },
    })
      .then((res) => {
        if (!res.ok) {
          throw new Error(`Spotify API error: ${res.status}`);
        }
        return res.json();
      })
      .then((data) => {
        console.log('✅ Spotify Profile loaded:', data);
        setSpotifyProfile(data);

        if (!data.email) {
          console.error('❌ Email not found in Spotify profile. Missing "user-read-email" scope?');
          setError('Email not found. Please re-authorize with correct scopes.');
          return;
        }

        const registerPayload = {
          userId: data.id,
          email: data.email,
          displayName: data.display_name || 'Unknown',
          accessToken: user.access_token,
          refreshToken: user.refresh_token,
          scopes: user.scope?.split(' ') || [],
        };

        console.log('📤 Sending register request:', registerPayload);

        fetch('http://localhost:8080/user/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(registerPayload),
        })
          .then(async (res) => {
            const result = await res.json();
            console.log('📥 Register response status:', res.status);
            console.log('📥 Register response body:', result);

            if (!res.ok) {
              console.error('❌ Register failed:', result);
              setRegisterStatus('failed');
              setError(`Registration failed: ${result.message || 'Unknown error'}`);
              return;
            }

            console.log('✅ User persisted successfully:', result);
            setRegisterStatus('success');
          })
          .catch((err) => {
            console.error('❌ Failed to persist tokens:', err);
            setRegisterStatus('failed');
            setError(`Network error: ${err.message}`);
          });
      })
      .catch((err) => {
        console.error('❌ Failed to fetch Spotify profile:', err);
        setError(`Failed to load profile: ${err.message}`);
      });
  }, [user]);

  const clientId = import.meta.env.VITE_SPOTIFY_CLIENT_ID;
const redirectUri = import.meta.env.VITE_SPOTIFY_REDIRECT_URI; // = http://127.0.0.1:5173/spotify-callback
const scopes = "user-read-email user-read-private playlist-read-private";

  const handleSpotifyConnect = () => {
    // route backend (backend spotify authorize endpoint)
    const url = `https://accounts.spotify.com/authorize?client_id=${clientId}&response_type=code&redirect_uri=${encodeURIComponent(redirectUri)}&scope=${encodeURIComponent(scopes)}`;
    window.location.href = url;
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 py-8 px-4">
      <div className="max-w-4xl mx-auto">
        {registerStatus === 'success' && (
          <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded-lg mb-4">
            ✅ Successfully registered in database
          </div>
        )}
        {registerStatus === 'failed' && (
          <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded-lg mb-4">
            ⚠️ Failed to register. Check console for details.
          </div>
        )}

        <div className="bg-white rounded-lg shadow-lg p-6 mb-8">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-4xl font-bold text-gray-800">Protected Page</h1>
              <h2 className="mt-2 text-gray-600">
                Welcome, {spotifyProfile?.display_name || 'User'}
              </h2>
              <button
        onClick={handleSpotifyConnect}
        className="bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-lg font-semibold"
      >
        Connect Spotify
      </button>
            </div>
            <button
              onClick={signoutCallback}
              className="flex items-center gap-2 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
            >
              <LogOut size={20} />
              Logout
            </button>
          </div>

          <div className="mt-4 p-4 bg-gray-50 rounded-md">
            <p className="text-sm text-gray-500 font-mono break-all">
              Access Token: {user?.access_token}
              <br />
              Refresh Token: {user?.refresh_token}
              <br />
              Expires In: {user?.expires_in}
              <br />
              Scopes: {user?.scope}
              <br />
              Spotify ID: {spotifyProfile?.id}
              <br />
              Email: {spotifyProfile?.email || '❌ Not available'}
              <br />
              Display Name: {spotifyProfile?.display_name}
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {playlists?.map((playlist) => (
            <div
              key={playlist.id}
              className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow p-6"
            >
              <h2 className="text-xl font-semibold text-gray-800 mb-2">
                {playlist?.name}
              </h2>
              <p className="text-sm text-gray-500 mb-2">
                {playlist.owner.display_name}
              </p>
              {playlist.description && (
                <p className="text-gray-600">{playlist?.description}</p>
              )}
            </div>
          ))}
        </div>

        {(!playlists || playlists.length === 0) && (
          <div className="text-center py-12">
            <p className="text-gray-500">No playlists found</p>
          </div>
        )}
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mt-4">
          {error}
        </div>
      )}
    </div>
  );
};

export default PostLoginPage;