import React, { createContext, ReactNode, useEffect, useState } from 'react';
import { User } from 'oidc-client-ts';
import { userManager } from '@/config/oidc.config';

interface AuthContextProps {
  user: User | null;
  signinRedirect: () => void;
  signoutRedirect: () => void;
  signoutCallback: () => void;
}

type AuthProviderProps = {
  children: ReactNode; // Explicitly define children as a prop
};

export const AuthContext = createContext<AuthContextProps>({} as AuthContextProps);

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true); // New loading state

  useEffect(() => {
    const handleUserLoaded = (loadedUser: User) => {
      setUser(loadedUser);
      setLoading(false);
    };

    const handleUserUnloaded = () => {
      setUser(null);
      setLoading(false);
    };

    userManager.events.addUserLoaded(handleUserLoaded);
    userManager.events.addUserUnloaded(handleUserUnloaded);

    userManager.getUser().then((loadedUser) => {
      setUser(loadedUser);
      setLoading(false); // Done loading
    });

    return () => {
      userManager.events.removeUserLoaded(handleUserLoaded);
      userManager.events.removeUserUnloaded(handleUserUnloaded);
    };
  }, []);

  const signinRedirect = () => userManager.signinRedirect();
  const signoutRedirect = () => userManager.signoutRedirect();
  const signoutCallback = () => userManager.signoutCallback();

  return (
    <AuthContext.Provider value={{ user, signinRedirect, signoutRedirect, signoutCallback }}>
      {loading ? <div>Loading...</div> : children} {/* Delay until ready */}
    </AuthContext.Provider>
  );
};