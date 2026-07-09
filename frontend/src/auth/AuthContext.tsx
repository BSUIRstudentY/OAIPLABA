import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, tokenStore } from '../api/client';
import type { AuthResponse, User } from '../types';

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      if (tokenStore.access) {
        try {
          const { data } = await api.get<User>('/auth/me');
          setUser(data);
        } catch {
          tokenStore.clear();
        }
      }
      setLoading(false);
    })();
  }, []);

  async function handleAuth(data: AuthResponse) {
    tokenStore.set({ accessToken: data.accessToken, refreshToken: data.refreshToken });
    setUser(data.user);
  }

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      async login(email, password) {
        const { data } = await api.post<AuthResponse>('/auth/login', { email, password });
        await handleAuth(data);
      },
      async register(email, password, displayName) {
        const { data } = await api.post<AuthResponse>('/auth/register', {
          email,
          password,
          displayName,
        });
        await handleAuth(data);
      },
      logout() {
        tokenStore.clear();
        setUser(null);
      },
      async refreshUser() {
        const { data } = await api.get<User>('/auth/me');
        setUser(data);
      },
    }),
    [user, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
