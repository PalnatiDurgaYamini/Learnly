import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { Icon } from '../ui';
import type { AuthResponse, Role } from '../types';

export default function Auth({ mode }: { mode: 'login' | 'register' }) {
  const { login } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ name: '', email: '', password: '', role: 'STUDENT' as Role });
  const [error, setError] = useState('');
  const [show, setShow] = useState(false);
  const [busy, setBusy] = useState(false);
  const reg = mode === 'register';

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(''); setBusy(true);
    try {
      const body = reg ? f : { email: f.email, password: f.password };
      login(await api<AuthResponse>(`/auth/${mode}`, { method: 'POST', body }));
      nav('/');
    } catch (err) { setError((err as Error).message); } finally { setBusy(false); }
  };

  return (
    <div className="authwrap">
      <aside className="authside">
        <h2>{reg ? 'Start your learning journey' : 'Welcome back'}</h2>
        <p>Courses, assignments, quizzes and certificates in one place.</p>
        <ul>{['Learn at your own pace', 'Track your progress', 'Earn verified certificates'].map(t => <li key={t}><Icon n="check" s={16} />{t}</li>)}</ul>
      </aside>
      <form className="authform" onSubmit={submit}>
        <h1>{reg ? 'Create your account' : 'Log in'}</h1>
        <p className="muted">{reg ? 'It only takes a minute.' : 'Enter your details to continue.'}</p>
        {error && <p className="error" role="alert">{error}</p>}
        {reg && (
          <div className="roles" role="group" aria-label="I am a">
            {([['STUDENT', 'Student', 'I want to learn'], ['INSTRUCTOR', 'Instructor', 'I want to teach']] as const).map(([r, t, d]) => (
              <button type="button" key={r} aria-pressed={f.role === r} className={`role${f.role === r ? ' on' : ''}`} onClick={() => setF({ ...f, role: r })}>
                <strong>{t}</strong><small>{d}</small></button>
            ))}
          </div>
        )}
        {reg && <label>Full name<input required autoComplete="name" value={f.name} onChange={e => setF({ ...f, name: e.target.value })} /></label>}
        <label>Email<input type="email" required autoComplete="email" value={f.email} onChange={e => setF({ ...f, email: e.target.value })} /></label>
        <label>Password
          <span className="pwd"><input type={show ? 'text' : 'password'} required minLength={6} autoComplete={reg ? 'new-password' : 'current-password'} value={f.password} onChange={e => setF({ ...f, password: e.target.value })} />
            <button type="button" className="icon dark" aria-label={show ? 'Hide password' : 'Show password'} onClick={() => setShow(!show)}><Icon n="eye" /></button></span>
        </label>
        {reg && <small className="muted">At least 6 characters.</small>}
        <button className="wide" disabled={busy}>{busy ? 'Please wait…' : reg ? 'Create account' : 'Log in'}</button>
        <p className="muted center">{reg ? <>Already registered? <Link to="/login">Log in</Link></> : <>New to Learnly? <Link to="/register">Create an account</Link></>}</p>
      </form>
    </div>
  );
}
