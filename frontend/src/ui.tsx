import type { ReactNode } from 'react';

const P: Record<string, string> = {
  home: 'M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z',
  book: 'M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2zM4 19a2 2 0 0 1 2-2h13',
  out: 'M9 4H5a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h4M16 8l4 4-4 4M20 12H9',
  play: 'M7 4l13 8-13 8z', plus: 'M12 5v14M5 12h14',
  search: 'M21 21l-4.3-4.3M17 10.5a6.5 6.5 0 1 1-13 0 6.5 6.5 0 0 1 13 0z',
  compass: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM15.5 8.5l-2 5-5 2 2-5z',
  check: 'M5 12l5 5L20 7', eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zM12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
  users: 'M17 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM21 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8',
  chart: 'M4 20V10M10 20V4M16 20v-8M22 20H2',
  award: 'M12 15a6 6 0 1 0 0-12 6 6 0 0 0 0 12zM8.5 14L7 21l5-3 5 3-1.5-7',
};
export const Icon = ({ n, s = 18 }: { n: string; s?: number }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={P[n]} /></svg>
);
const hue = (id: number) => 165 + ((id * 37) % 70);
export const Thumb = ({ id, title, large }: { id: number; title: string; large?: boolean }) => (
  <div className={`thumb${large ? ' lg' : ''}`} style={{ background: `linear-gradient(135deg,hsl(${hue(id)} 55% 24%),hsl(${hue(id) + 28} 60% 40%))` }}>
    <span>{title.slice(0, 2).toUpperCase()}</span>
  </div>
);
export const Progress = ({ value }: { value: number }) => (
  <div className="bar" role="progressbar" aria-valuenow={value} aria-valuemin={0} aria-valuemax={100}><div style={{ width: `${value}%` }} /><span>{value}%</span></div>
);
export const Skeleton = ({ n = 3 }: { n?: number }) => (
  <div className="grid">{Array.from({ length: n }, (_, i) => <div key={i} className="card sk" />)}</div>
);
export const Empty = ({ icon, title, text, children }: { icon: string; title: string; text: string; children?: ReactNode }) => (
  <div className="empty"><div className="empty-i"><Icon n={icon} s={26} /></div><h3>{title}</h3><p className="muted">{text}</p>{children}</div>
);
export const Avatar = ({ name }: { name: string }) => (
  <span className="avatar">{name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()}</span>
);
