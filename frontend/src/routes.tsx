import React, { useContext, useEffect } from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import { AuthProvider, AuthContext } from '@/context/AuthProvider';
import { userManager } from '@/config/oidc.config';
import LoginPage from '@/pages/LoginPage';
import PostLoginPage from '@/pages/PostLoginPage';
import UserInfoPage from './pages/UserInfoPage';
import PlaylistPage from './pages/PlayListPage';
import PlaylistDetailPage from './pages/PlaylistDetailPage';

const CallbackPage: React.FC = () => {
  useEffect(() => {
    userManager.signinRedirectCallback().then(() => {
      window.location.href = '/main';
    });
  }, []);

  return <div>Loading...</div>;
};

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useContext(AuthContext);
  if (!user) {
    return <Navigate to="/login" />;
  }
  return <>{children}</>;
};

export const RoutesProvider: React.FC = () => {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/callback" element={<CallbackPage />} />
          <Route 
          path='/userinfo'
          element={
            <ProtectedRoute>
              <UserInfoPage></UserInfoPage>
            </ProtectedRoute>
          }
          />
          <Route 
          path='/playlists'
          element={
            <ProtectedRoute>
              <PlaylistPage></PlaylistPage>
            </ProtectedRoute>
          }
          />
          <Route 
          path='/playlist/:id'
          element={
            <ProtectedRoute>
              <PlaylistDetailPage></PlaylistDetailPage>
            </ProtectedRoute>
          }
          />
          <Route
            path="/main"
            element={
              <ProtectedRoute>
                <PostLoginPage />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/login" />} />
        </Routes>
      </Router>
    </AuthProvider>
  )
};