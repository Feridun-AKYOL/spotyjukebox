import { useState } from "react";
import QrScanner from "react-qr-scanner";
import { useNavigate } from "react-router-dom";

export default function ClientPage() {
  const navigate = useNavigate();
  const [cameraAllowed, setCameraAllowed] = useState(true);
  const [scannedId, setScannedId] = useState<string | null>(null);

  const handleScan = (data: string | null) => {
    if (data) {
      console.log("✅ QR detected:", data);
      setScannedId(data);
      // QR içeriğini doğrula (örnek: sadece ownerId içeriyor)
      if (data.startsWith("http")) {
        // QR doğrudan tam URL içeriyorsa
        const url = new URL(data);
        const ownerId = url.searchParams.get("ownerId");
        if (ownerId) navigate(`/client/session?ownerId=${ownerId}`);
      } else {
        // sadece ID içeriyorsa
        navigate(`/client/session?ownerId=${data}`);
      }
    }
  };

  const handleError = (err: any) => {
    console.error("QR Error:", err);
    setCameraAllowed(false);
  };

  return (
    <div className="min-h-screen bg-[#121212] text-gray-200 flex flex-col items-center justify-center px-4">
      {/* Başlık */}
      <h1 className="text-3xl font-extrabold text-green-400 mb-3">
        Join the Jukebox
      </h1>
      <p className="text-gray-400 text-center mb-6">
        Point your camera at the QR code to join the live session 🎶
      </p>

      {/* QR Kamera */}
      {cameraAllowed ? (
        <div className="bg-gray-900 p-3 rounded-2xl border border-gray-700 shadow-lg">
          <QrScanner
            delay={300}
            style={{ width: 300, height: 300 }}
            onError={handleError}
            onScan={handleScan}
            constraints={{ facingMode: "environment" }}
          />
        </div>
      ) : (
        <div className="text-center text-red-400 mt-8">
          <p className="font-semibold text-lg mb-2">Camera access denied</p>
          <p className="text-gray-400">
            Please allow camera permissions and reload the page.
          </p>
        </div>
      )}

      {/* QR Okunduğunda */}
      {scannedId && (
        <div className="mt-6 text-center">
          <h2 className="text-green-400 text-xl font-semibold">
            Connecting to session...
          </h2>
          <p className="text-gray-500 text-sm mt-1">{scannedId}</p>
        </div>
      )}

      {/* Alt bilgi */}
      <div className="mt-12 text-gray-500 text-sm text-center">
        <p>Powered by Spotify API · Bithub Jukebox</p>
      </div>
    </div>
  );
}
