import { useCallback, useEffect, useState } from 'react';

const BASE: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

export const getToken = () => localStorage.getItem('token');

async function request(path: string, method = 'GET', body?: unknown): Promise<Response> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const t = getToken();
  if (t) headers.Authorization = `Bearer ${t}`;
  const res = await fetch(BASE + path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  if (res.status === 401 && t) {           // token expired -> back to login
    localStorage.clear();
    window.location.href = '/login';
  }
  if (!res.ok) {
    let msg = res.statusText;
    try { msg = (await res.json()).message || msg; } catch { /* no json body */ }
    throw new Error(msg);
  }
  return res;
}

export async function api<T = unknown>(path: string, opts: { method?: string; body?: unknown } = {}): Promise<T> {
  const res = await request(path, opts.method, opts.body);
  const text = await res.text();
  return (text ? JSON.parse(text) : null) as T;
}

export async function download(path: string, filename: string) {
  const blob = await (await request(path)).blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}

/** Open a protected file (such as a PDF) in a new browser tab without downloading it. */
export async function openFile(path: string) {
  const w = window.open('', '_blank');
  try {
    const blob = await (await request(path)).blob();
    const url = URL.createObjectURL(blob);
    if (w) w.location.href = url; else window.location.href = url;
  } catch (e) { w?.close(); throw e; }
}

/** GET helper. Pass null to skip fetching. */
export function useFetch<T>(path: string | null) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState('');
  const load = useCallback(() => {
    if (!path) return;
    setError('');
    api<T>(path).then(setData).catch((e: Error) => setError(e.message));
  }, [path]);
  useEffect(() => { load(); }, [load]);
  return { data, error, reload: load };
}

/** Amounts are stored in paise (INR). */
export const money = (cents: number) => (cents > 0 ? `₹${(cents / 100).toFixed(2)}` : 'Free');

/** WebSocket URL of the backend (same host as the REST API, without the /api suffix). */
export const WS_URL = BASE.replace(/\/api\/?$/, '').replace(/^http/, 'ws') + '/ws-lms';
export const fmt = (iso: string | null) => (iso ? new Date(iso).toLocaleString() : '—');
