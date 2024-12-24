import { AuthContext } from "@/context/AuthProvider";
import { useContext } from "react";
import { LogIn } from 'lucide-react';

const LoginPage = () => {
  const { signinRedirect } = useContext(AuthContext);

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50 flex flex-col items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-lg p-8 max-w-md w-full mx-auto text-center">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">Welcome</h1>
        <p className="text-gray-600 mb-8">Please sign in to continue</p>
        
        <button
          onClick={signinRedirect}
          className="flex items-center justify-center gap-2 w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-6 rounded-lg transition-colors"
        >
          <LogIn size={20} />
          Sign In
        </button>
      </div>
    </div>
  );
};

export default LoginPage;