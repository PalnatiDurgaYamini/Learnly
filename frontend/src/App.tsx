import type { ReactElement } from 'react';
import { Link, Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import { Avatar, Icon } from './ui';
import Auth from './pages/Auth';
import Courses from './pages/Courses';
import CoursePage from './pages/Course';
import Verify from './pages/Verify';
import Welcome from './pages/Welcome';
import Admin from './pages/Admin';
import Certificates from './pages/Certificates';

function Protected({ children }: { children: ReactElement }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/login" replace />;
}

export default function App() {
  const { user, logout } = useAuth();
  const student = user?.role === 'STUDENT';
  const links = [
    { to: '/', icon: 'home', label: student ? 'My learning' : user?.role === 'ADMIN' ? 'Admin' : 'My courses', end: true },
    ...(student ? [{ to: '/?view=browse', icon: 'compass', label: 'Browse', end: false }, { to: '/certificates', icon: 'award', label: 'Certificates', end: true }] : []),
  ];
  const routes = (
    <Routes>
      <Route path="/login" element={<Auth mode="login" />} />
      <Route path="/register" element={<Auth mode="register" />} />
      <Route path="/verify/:code" element={<Verify />} />
      <Route path="/" element={!user ? <Welcome /> : user.role === 'ADMIN' ? <Admin /> : <Courses />} />
      <Route path="/certificates" element={<Protected><Certificates /></Protected>} />
      <Route path="/courses/:id" element={<Protected><CoursePage /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );

  if (!user) return (
    <div className="public">
      <header className="topbar"><Link to="/" className="brand"><Icon n="book" s={22} /> Learnly</Link><span className="spacer" />
        <Link to="/login" className="btn ghost">Log in</Link><Link to="/register" className="btn">Get started</Link></header>
      <main className="content">{routes}</main>
    </div>
  );

  return (
    <div className="shell">
      <aside className="side">
        <Link to="/" className="brand"><Icon n="book" s={22} /> Learnly</Link>
        <nav aria-label="Main">{links.map(l => (
          <NavLink key={l.label} to={l.to} end={l.end} className="navlink"><Icon n={l.icon} />{l.label}</NavLink>
        ))}</nav>
        <span className="spacer" />
        <div className="me"><Avatar name={user.name} /><div><strong>{user.name}</strong><small>{user.role.toLowerCase()}</small></div>
          <button className="icon" onClick={logout} aria-label="Log out" title="Log out"><Icon n="out" /></button></div>
      </aside>
      <main className="content">{routes}</main>
      <nav className="tabbar" aria-label="Mobile">{links.map(l => (
        <NavLink key={l.label} to={l.to} end={l.end}><Icon n={l.icon} s={22} />{l.label}</NavLink>
      ))}<button onClick={logout}><Icon n="out" s={22} />Log out</button></nav>
    </div>
  );
}
