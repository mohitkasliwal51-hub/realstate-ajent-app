'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';
import { AuthState, UserProfile } from './authTypes';
import { apiClient } from '../api/client';

interface AuthContextType extends AuthState {
  login: (token: string, user: UserProfile) => void;
  logout: () => void;
  setUser: (user: UserProfile | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AuthState>({
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: true,
  });

  useEffect(() => {
    const initializeAuth = () => {
      try {
        const storedToken = localStorage.getItem('stayfile_token');
        const storedUser = localStorage.getItem('stayfile_user');

        if (storedToken && storedUser) {
          const user = JSON.parse(storedUser) as UserProfile;
          setState({
            user,
            token: storedToken,
            isAuthenticated: true,
            isLoading: false,
          });
        } else {
          setState({
            user: null,
            token: null,
            isAuthenticated: false,
            isLoading: false,
          });
        }
      } catch (err) {
        console.error('Failed to parse saved auth state', err);
        localStorage.removeItem('stayfile_token');
        localStorage.removeItem('stayfile_user');
        setState({
          user: null,
          token: null,
          isAuthenticated: false,
          isLoading: false,
        });
      }
    };

    initializeAuth();
  }, []);

  const login = (token: string, user: UserProfile) => {
    localStorage.setItem('stayfile_token', token);
    localStorage.setItem('stayfile_user', JSON.stringify(user));
    setState({
      user,
      token,
      isAuthenticated: true,
      isLoading: false,
    });
  };

  const logout = () => {
    localStorage.removeItem('stayfile_token');
    localStorage.removeItem('stayfile_user');
    setState({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    });
    if (typeof window !== 'undefined') {
      window.location.href = '/login';
    }
  };

  const setUser = (user: UserProfile | null) => {
    if (user) {
      localStorage.setItem('stayfile_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('stayfile_user');
    }
    setState((prev) => ({
      ...prev,
      user,
      isAuthenticated: !!user && !!prev.token,
    }));
  };

  return (
    <AuthContext.Provider value={{ ...state, login, logout, setUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
