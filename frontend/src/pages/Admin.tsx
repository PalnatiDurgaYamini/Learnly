import { useState } from 'react';
import { Link } from 'react-router-dom';
import { fmt, money, useFetch } from '../api';
import { useAuth } from '../auth';
import { Avatar, Empty, Skeleton } from '../ui';
import JobMonitor from './JobMonitor';
import type { Course, Payment, User } from '../types';

interface Stats { students: number; instructors: number; admins: number; courses: number; enrollments: number; paidPayments: number; revenueCents: number }
const Stat = ({ label, value }: { label: string; value: string | number }) => (
  <div className="stat"><span className="muted">{label}</span><strong>{value}</strong></div>
);
const TABS = ['users', 'courses', 'payments', 'jobs'] as const;

export default function Admin() {
  const { user } = useAuth();
  const { data: s } = useFetch<Stats>('/admin/stats');
  const { data: users } = useFetch<User[]>('/admin/users');
  const { data: courses } = useFetch<Course[]>('/courses');
  const { data: pays } = useFetch<Payment[]>('/admin/payments');
  const [tab, setTab] = useState<(typeof TABS)[number]>('users');
  const [q, setQ] = useState('');
  const ql = q.toLowerCase();
  const u = (users ?? []).filter(x => (x.name + x.email + x.role).toLowerCase().includes(ql));
  const c = (courses ?? []).filter(x => (x.title + x.instructor.name).toLowerCase().includes(ql));

  return (
    <div>
      <h1>Admin overview</h1><p className="lead">Signed in as {user?.name}. Everything happening on the platform.</p>
      <div className="stats">
        <Stat label="Students" value={s?.students ?? '–'} /><Stat label="Instructors" value={s?.instructors ?? '–'} />
        <Stat label="Courses" value={s?.courses ?? '–'} /><Stat label="Enrollments" value={s?.enrollments ?? '–'} />
        <Stat label="Revenue" value={s ? money(s.revenueCents) : '–'} />
      </div>
      <div className="tabs">{TABS.map(t => <button key={t} className={t === tab ? 'active' : ''} onClick={() => setTab(t)}>{t}</button>)}</div>
      {(tab === 'users' || tab === 'courses') && <input className="tsearch" placeholder={`Search ${tab}`} aria-label={`Search ${tab}`} value={q} onChange={e => setQ(e.target.value)} />}

      {tab === 'users' && (!users ? <Skeleton n={1} /> : u.length === 0 ? <Empty icon="search" title="No users found" text="Try a different search." /> : (
        <div className="tablewrap"><table className="tbl"><thead><tr><th>User</th><th>Email</th><th>Role</th></tr></thead>
          <tbody>{u.map(x => <tr key={x.id}><td><span className="cell"><Avatar name={x.name} />{x.name}</span></td><td>{x.email}</td><td><span className="badge">{x.role}</span></td></tr>)}</tbody></table></div>
      ))}
      {tab === 'courses' && (!courses ? <Skeleton n={1} /> : c.length === 0 ? <Empty icon="book" title="No courses found" text="Nothing matches your search." /> : (
        <div className="tablewrap"><table className="tbl"><thead><tr><th>Course</th><th>Instructor</th><th>Price</th></tr></thead>
          <tbody>{c.map(x => <tr key={x.id}><td><Link to={`/courses/${x.id}`}>{x.title}</Link></td><td>{x.instructor.name}</td><td>{money(x.priceCents)}</td></tr>)}</tbody></table></div>
      ))}
      {tab === 'payments' && (!pays ? <Skeleton n={1} /> : pays.length === 0 ? <Empty icon="chart" title="No payments yet" text="Payments appear here after students buy paid courses." /> : (
        <div className="tablewrap"><table className="tbl"><thead><tr><th>Student</th><th>Course</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead>
          <tbody>{pays.map(p => <tr key={p.id}><td>{p.student?.name}</td><td>{p.course?.title}</td><td>{money(p.amountCents)}</td>
            <td><span className={`badge ${p.status === 'PAID' ? 'ok' : 'warn'}`}>{p.status}</span></td><td>{fmt(p.createdAt)}</td></tr>)}</tbody></table></div>
      ))}
      {tab === 'jobs' && <JobMonitor />}
    </div>
  );
}
