import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, money, useFetch } from '../api';
import { useAuth } from '../auth';
import { Empty, Icon, Progress, Skeleton, Thumb } from '../ui';
import type { Course, EnrolledCourse } from '../types';

const greet = () => { const h = new Date().getHours(); return h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening'; };

function Stat({ label, value }: { label: string; value: string | number }) {
  return <div className="stat"><span className="muted">{label}</span><strong>{value}</strong></div>;
}

function CourseCard({ c, progress, enrolled }: { c: Course; progress?: number; enrolled?: boolean }) {
  return (
    <Link to={`/courses/${c.id}`} className="card course">
      <Thumb id={c.id} title={c.title} />
      <div className="body">
        <h3>{c.title}</h3>
        <p className="muted">{c.instructor.name}</p>
        {progress !== undefined ? <Progress value={progress} />
          : <div className="row"><span className="price">{enrolled ? 'Enrolled' : money(c.priceCents)}</span><span className="go">View <Icon n="play" s={12} /></span></div>}
      </div>
    </Link>
  );
}

export default function Courses() {
  const { user } = useAuth();
  const student = user?.role === 'STUDENT';
  const [params] = useSearchParams();
  const browse = params.get('view') === 'browse';
  const { data: all, reload } = useFetch<Course[]>(student ? '/courses' : '/courses/mine');
  const { data: mine } = useFetch<EnrolledCourse[]>(student ? '/enrollments/mine' : null);
  const [q, setQ] = useState('');
  const [f, setF] = useState({ title: '', description: '', price: '0' });
  const [msg, setMsg] = useState('');
  const first = user?.name.split(' ')[0];

  if (student && browse) {
    const ids = new Set((mine ?? []).map(e => e.course.id));
    const list = (all ?? []).filter(c => (c.title + c.instructor.name).toLowerCase().includes(q.toLowerCase()));
    return (
      <div>
        <h1>Explore courses</h1><p className="lead">Pick something new to learn today.</p>
        <label className="search"><Icon n="search" /><input placeholder="Search courses or instructors" aria-label="Search courses" value={q} onChange={e => setQ(e.target.value)} /></label>
        {!all ? <Skeleton /> : list.length === 0
          ? <Empty icon="search" title="No courses found" text="Try a different search term." />
          : <div className="grid">{list.map(c => <CourseCard key={c.id} c={c} enrolled={ids.has(c.id)} />)}</div>}
      </div>
    );
  }

  if (student) {
    const m = mine ?? [];
    const next = m.filter(e => e.progress < 100).sort((a, b) => b.progress - a.progress)[0];
    const avg = m.length ? Math.round(m.reduce((s, e) => s + e.progress, 0) / m.length) : 0;
    const rec = (all ?? []).filter(c => !m.some(e => e.course.id === c.id)).slice(0, 3);
    return (
      <div>
        <h1>{greet()}, {first}</h1><p className="lead">Here's where you left off.</p>
        <div className="stats">
          <Stat label="Enrolled courses" value={m.length} /><Stat label="Average progress" value={`${avg}%`} />
          <Stat label="Completed" value={m.filter(e => e.progress === 100).length} />
        </div>
        {next && (
          <section className="card continue">
            <Thumb id={next.course.id} title={next.course.title} large />
            <div><span className="eyebrow">Continue learning</span><h2>{next.course.title}</h2>
              <p className="muted">{next.course.instructor.name}</p><Progress value={next.progress} />
              <Link to={`/courses/${next.course.id}`} className="btn"><Icon n="play" s={14} /> Resume course</Link></div>
          </section>
        )}
        <h2>My courses</h2>
        {!mine ? <Skeleton /> : m.length === 0
          ? <Empty icon="book" title="Nothing here yet" text="Enroll in a course to start tracking your progress."><Link to="/?view=browse" className="btn">Browse courses</Link></Empty>
          : <div className="grid">{m.map(e => <CourseCard key={e.course.id} c={e.course} progress={e.progress} />)}</div>}
        {rec.length > 0 && <><h2>Recommended for you</h2><div className="grid">{rec.map(c => <CourseCard key={c.id} c={c} />)}</div></>}
      </div>
    );
  }

  const create = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await api('/courses', { method: 'POST', body: { title: f.title, description: f.description, priceCents: Math.round(parseFloat(f.price || '0') * 100) } });
      setF({ title: '', description: '', price: '0' }); setMsg(''); reload();
    } catch (err) { setMsg((err as Error).message); }
  };
  return (
    <div>
      <h1>{greet()}, {first}</h1><p className="lead">Manage your courses and students.</p>
      <div className="stats"><Stat label="Courses" value={all?.length ?? 0} /><Stat label="Paid courses" value={(all ?? []).filter(c => c.priceCents > 0).length} /></div>
      <div className="split">
        <div>
          <h2>Your courses</h2>
          {!all ? <Skeleton n={2} /> : all.length === 0
            ? <Empty icon="plus" title="Create your first course" text="Use the form to publish a course, then add lessons and quizzes." />
            : <div className="grid">{all.map(c => <CourseCard key={c.id} c={c} />)}</div>}
        </div>
        <form className="card" onSubmit={create}>
          <h3>New course</h3>
          {msg && <p className="error" role="alert">{msg}</p>}
          <label>Title<input required value={f.title} onChange={e => setF({ ...f, title: e.target.value })} /></label>
          <label>Description<textarea value={f.description} onChange={e => setF({ ...f, description: e.target.value })} /></label>
          <label>Price (₹, 0 = free)<input type="number" min="0" step="0.01" value={f.price} onChange={e => setF({ ...f, price: e.target.value })} /></label>
          <button>Create course</button>
        </form>
      </div>
    </div>
  );
}
