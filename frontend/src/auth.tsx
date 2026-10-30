import { createContext, useContext, useState, type ReactNode } from 'react';
import type { AuthResponse, User } from './types';

interface Ctx { user: User | null; login: (d: AuthResponse) => void; logout: () => void }
const AuthCtx = createContext<Ctx>({ user: null, login: () => {}, logout: () => {} });
export const useAuth = () => useContext(AuthCtx);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    try { return JSON.parse(localStorage.getItem('user') || 'null'); } catch { return null; }
  });
  const login = (d: AuthResponse) => {
    localStorage.setItem('token', d.token);
    localStorage.setItem('user', JSON.stringify(d.user));
    setUser(d.user);
  };
  const logout = () => { localStorage.clear(); setUser(null); };
  return <AuthCtx.Provider value={{ user, login, logout }}>{children}</AuthCtx.Provider>;
}
