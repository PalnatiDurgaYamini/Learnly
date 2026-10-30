import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, download, fmt, money, openFile, useFetch } from '../api';
import { useAuth } from '../auth';
import { payForCourse } from '../payment';
import { Icon, Thumb } from '../ui';
import type {
  Assessment, Assignment, Attempt, Certificate, Course, EnrolledCourse, ForumPost,
  Material, Payment, Question, StudentProgress, Submission,
} from '../types';

/** Returns the 11-character video id for youtube.com / youtu.be links, otherwise null. */
const ytId = (u: string | null | undefined) => {
  const m = (u ?? '').match(/(?:youtu\.be\/|youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/))([\w-]{11})/);
  return m ? m[1] : null;
};

interface P { course: Course; owner: boolean; student: boolean }

/* ------------------------------ Materials ------------------------------ */
function Materials({ course, owner, student }: P) {
  const { data: items, reload, error } = useFetch<Material[]>(`/courses/${course.id}/materials`);
  const { data: done, reload: reloadDone } = useFetch<number[]>(student ? `/courses/${course.id}/completed-materials` : null);
  const [f, setF] = useState({ title: '', content: '', url: '' });

  const add = async (e: FormEvent) => {
    e.preventDefault();
    await api(`/courses/${course.id}/materials`, { method: 'POST', body: f });
    setF({ title: '', content: '', url: '' });
    reload();
  };
  const complete = async (id: number) => { await api(`/materials/${id}/complete`, { method: 'POST' }); reloadDone(); };

  if (error) return <p className="error">{error}</p>;
  return (
    <div>
      {(items ?? []).map(m => {
        const isDone = (done ?? []).includes(m.id);
        return (
          <div className="card" key={m.id}>
            <h3>{m.title}</h3>
            <p>{m.content}</p>
            {ytId(m.url) && (
              <div style={{ position: 'relative', paddingTop: '56.25%', margin: '10px 0', borderRadius: 10, overflow: 'hidden', background: '#000' }}>
                <iframe title={m.title} src={`https://www.youtube-nocookie.com/embed/${ytId(m.url)}`} loading="lazy" allowFullScreen
                  allow="accelerometer; encrypted-media; picture-in-picture; fullscreen"
                  style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', border: 0 }} />
              </div>
            )}
            {m.url && <a href={m.url} target="_blank" rel="noreferrer">{ytId(m.url) ? 'Watch on YouTube ↗' : 'Open resource ↗'}</a>}
            {student && (
              <label className="check">
                <input type="checkbox" checked={isDone} disabled={isDone} onChange={() => complete(m.id)} /> Mark as completed
              </label>
            )}
          </div>
        );
      })}
      {items?.length === 0 && <p className="muted">No materials yet.</p>}
      {owner && (
        <form className="card" onSubmit={add}>
          <h3>Add material</h3>
          <input placeholder="Title" required value={f.title} onChange={e => setF({ ...f, title: e.target.value })} />
          <textarea placeholder="Content / notes" value={f.content} onChange={e => setF({ ...f, content: e.target.value })} />
          <input placeholder="Link (video, PDF, slides…)" value={f.url} onChange={e => setF({ ...f, url: e.target.value })} />
          <button>Add</button>
        </form>
      )}
    </div>
  );
}

/* ------------------------------ Assignments ------------------------------ */
function Submissions({ assignment }: { assignment: Assignment }) {
  const { data, reload, error } = useFetch<Submission[]>(`/assignments/${assignment.id}/submissions`);
  const [grades, setGrades] = useState<Record<number, { grade: string; feedback: string }>>({});

  const save = async (s: Submission) => {
    const g = grades[s.id] ?? { grade: String(s.grade ?? ''), feedback: s.feedback ?? '' };
    try {
      await api(`/submissions/${s.id}/grade`, { method: 'PUT', body: { grade: Number(g.grade), feedback: g.feedback } });
      reload();
    } catch (e) { alert((e as Error).message); }
  };

  if (error) return <p className="error">{error}</p>;
  return (
    <div className="sub">
      {(data ?? []).map(s => {
        const g = grades[s.id] ?? { grade: s.grade?.toString() ?? '', feedback: s.feedback ?? '' };
        return (
          <div key={s.id} className="card">
            <strong>{s.student.name}</strong> <small className="muted">{fmt(s.submittedAt)}</small>
            <p>{s.content}</p>
            <div className="row">
              <input type="number" min={0} max={assignment.maxPoints} placeholder={`Grade /${assignment.maxPoints}`} value={g.grade}
                onChange={e => setGrades({ ...grades, [s.id]: { ...g, grade: e.target.value } })} />
              <input placeholder="Feedback" value={g.feedback}
                onChange={e => setGrades({ ...grades, [s.id]: { ...g, feedback: e.target.value } })} />
              <button onClick={() => save(s)}>Save grade</button>
            </div>
          </div>
        );
      })}
      {data?.length === 0 && <p className="muted">No submissions yet.</p>}
    </div>
  );
}

function SubmitBox({ assignment, existing, onDone }: { assignment: Assignment; existing?: Submission; onDone: () => void }) {
  const [text, setText] = useState(existing?.content ?? '');
  const [error, setError] = useState('');
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try { await api(`/assignments/${assignment.id}/submit`, { method: 'POST', body: { content: text } }); setError(''); onDone(); }
    catch (err) { setError((err as Error).message); }
  };
  return (
    <form onSubmit={submit}>
      {existing && (
        <p className="muted">
          Submitted {fmt(existing.submittedAt)} ·{' '}
          {existing.grade !== null ? <strong>Grade: {existing.grade}/{assignment.maxPoints}</strong> : 'awaiting grade'}
          {existing.feedback && <> · Feedback: {existing.feedback}</>}
        </p>
      )}
      {error && <p className="error">{error}</p>}
      <textarea required placeholder="Your answer or a link to your work" value={text} onChange={e => setText(e.target.value)} />
      <button>{existing ? 'Resubmit' : 'Submit'}</button>
    </form>
  );
}

function Assignments({ course, owner, student }: P) {
  const { data: items, reload, error } = useFetch<Assignment[]>(`/courses/${course.id}/assignments`);
  const { data: mine, reload: reloadMine } = useFetch<Submission[]>(student ? '/submissions/mine' : null);
  const [open, setOpen] = useState<number | null>(null);
  const [f, setF] = useState({ title: '', description: '', dueDate: '', maxPoints: '100' });

  const add = async (e: FormEvent) => {
    e.preventDefault();
    await api(`/courses/${course.id}/assignments`, {
      method: 'POST',
      body: { title: f.title, description: f.description, dueDate: f.dueDate || null, maxPoints: Number(f.maxPoints) },
    });
    setF({ title: '', description: '', dueDate: '', maxPoints: '100' });
    reload();
  };

  if (error) return <p className="error">{error}</p>;
  return (
    <div>
      {(items ?? []).map(a => (
        <div className="card" key={a.id}>
          <h3>{a.title} <small className="muted">({a.maxPoints} pts)</small></h3>
          <p>{a.description}</p>
          <p className="muted">Due: {a.dueDate ? fmt(a.dueDate) : 'no deadline'}</p>
          {student && <SubmitBox assignment={a} existing={(mine ?? []).find(s => s.assignment.id === a.id)} onDone={reloadMine} />}
          {owner && (
            <>
              <button className="link" onClick={() => setOpen(open === a.id ? null : a.id)}>{open === a.id ? 'Hide' : 'View'} submissions</button>
              {open === a.id && <Submissions assignment={a} />}
            </>
          )}
        </div>
      ))}
      {items?.length === 0 && <p className="muted">No assignments yet.</p>}
      {owner && (
        <form className="card" onSubmit={add}>
          <h3>New assignment</h3>
          <input placeholder="Title" required value={f.title} onChange={e => setF({ ...f, title: e.target.value })} />
          <textarea placeholder="Instructions" value={f.description} onChange={e => setF({ ...f, description: e.target.value })} />
          <div className="row">
            <label>Due date<input type="datetime-local" value={f.dueDate} onChange={e => setF({ ...f, dueDate: e.target.value })} /></label>
            <label>Max points<input type="number" min={1} value={f.maxPoints} onChange={e => setF({ ...f, maxPoints: e.target.value })} /></label>
          </div>
          <button>Create assignment</button>
        </form>
      )}
    </div>
  );
}

/* ------------------------------ Quizzes ------------------------------ */
function QuizTake({ quiz }: { quiz: Assessment }) {
  const { data: qs } = useFetch<Question[]>(`/assessments/${quiz.id}/questions`);
  const { data: prev, reload } = useFetch<Attempt[]>(`/assessments/${quiz.id}/attempts/mine`);
  const [ans, setAns] = useState<Record<number, string>>({});
  const [result, setResult] = useState<Attempt | null>(null);

  const submit = async () => {
    setResult(await api<Attempt>(`/assessments/${quiz.id}/attempt`, { method: 'POST', body: { answers: ans } }));
    reload();
  };
  const best = (prev ?? []).reduce((m, a) => Math.max(m, a.score), 0);

  return (
    <div className="sub">
      {(qs ?? []).map((q, i) => (
        <div key={q.id} className="q">
          <p><strong>{i + 1}. {q.text}</strong></p>
          {(['A', 'B', 'C', 'D'] as const).map(k => (
            <label key={k} className="check">
              <input type="radio" name={`q${q.id}`} checked={ans[q.id] === k} onChange={() => setAns({ ...ans, [q.id]: k })} />
              {q[`option${k}` as 'optionA']}
            </label>
          ))}
        </div>
      ))}
      <button onClick={submit}>Submit quiz</button>
      {result && <p><strong>Score: {result.score}/{result.total}</strong></p>}
      {prev && prev.length > 0 && <p className="muted">Attempts: {prev.length} · Best: {best}/{prev[0].total}</p>}
    </div>
  );
}

const blankQ = { text: '', optionA: '', optionB: '', optionC: '', optionD: '', correct: 'A' };

function QuizCreate({ courseId, onDone }: { courseId: number; onDone: () => void }) {
  const [title, setTitle] = useState('');
  const [qs, setQs] = useState([{ ...blankQ }]);
  const setQ = (i: number, patch: Partial<typeof blankQ>) => setQs(qs.map((q, j) => (j === i ? { ...q, ...patch } : q)));

  const save = async (e: FormEvent) => {
    e.preventDefault();
    await api(`/courses/${courseId}/assessments`, { method: 'POST', body: { title, questions: qs } });
    setTitle(''); setQs([{ ...blankQ }]); onDone();
  };
  return (
    <form className="card" onSubmit={save}>
      <h3>New quiz</h3>
      <input placeholder="Quiz title" required value={title} onChange={e => setTitle(e.target.value)} />
      {qs.map((q, i) => (
        <div key={i} className="q">
          <input placeholder={`Question ${i + 1}`} required value={q.text} onChange={e => setQ(i, { text: e.target.value })} />
          {(['A', 'B', 'C', 'D'] as const).map(k => (
            <input key={k} placeholder={`Option ${k}`} required value={q[`option${k}` as 'optionA']}
              onChange={e => setQ(i, { [`option${k}`]: e.target.value } as Partial<typeof blankQ>)} />
          ))}
          <label>Correct answer
            <select value={q.correct} onChange={e => setQ(i, { correct: e.target.value })}>
              {['A', 'B', 'C', 'D'].map(k => <option key={k}>{k}</option>)}
            </select>
          </label>
        </div>
      ))}
      <div className="row">
        <button type="button" onClick={() => setQs([...qs, { ...blankQ }])}>+ Add question</button>
        <button>Save quiz</button>
      </div>
    </form>
  );
}

function Quizzes({ course, owner, student }: P) {
  const { data, reload, error } = useFetch<Assessment[]>(`/courses/${course.id}/assessments`);
  const [open, setOpen] = useState<number | null>(null);
  if (error) return <p className="error">{error}</p>;
  return (
    <div>
      {(data ?? []).map(q => (
        <div key={q.id} className="card">
          <h3>{q.title}</h3>
          {student && (
            <>
              <button onClick={() => setOpen(open === q.id ? null : q.id)}>{open === q.id ? 'Close' : 'Take quiz'}</button>
              {open === q.id && <QuizTake quiz={q} />}
            </>
          )}
        </div>
      ))}
      {data?.length === 0 && <p className="muted">No quizzes yet.</p>}
      {owner && <QuizCreate courseId={course.id} onDone={reload} />}
    </div>
  );
}

/* ------------------------------ Forum ------------------------------ */
function Thread({ post }: { post: ForumPost }) {
  const { data, reload } = useFetch<ForumPost[]>(`/posts/${post.id}/replies`);
  const [text, setText] = useState('');
  const reply = async (e: FormEvent) => {
    e.preventDefault();
    await api(`/posts/${post.id}/replies`, { method: 'POST', body: { body: text } });
    setText(''); reload();
  };
  return (
    <div className="sub">
      {(data ?? []).map(r => (
        <p key={r.id}><strong>{r.author.name}</strong> <small className="muted">{fmt(r.createdAt)}</small><br />{r.body}</p>
      ))}
      <form className="row" onSubmit={reply}>
        <input placeholder="Write a reply…" required value={text} onChange={e => setText(e.target.value)} />
        <button>Reply</button>
      </form>
    </div>
  );
}

function Forum({ course }: P) {
  const { data, reload, error } = useFetch<ForumPost[]>(`/courses/${course.id}/posts`);
  const [open, setOpen] = useState<number | null>(null);
  const [f, setF] = useState({ title: '', body: '' });
  const post = async (e: FormEvent) => {
    e.preventDefault();
    await api(`/courses/${course.id}/posts`, { method: 'POST', body: f });
    setF({ title: '', body: '' }); reload();
  };
  if (error) return <p className="error">{error}</p>;
  return (
    <div>
      {(data ?? []).map(p => (
        <div className="card" key={p.id}>
          <h3>{p.title}</h3>
          <small className="muted">{p.author.name} · {fmt(p.createdAt)}</small>
          <p>{p.body}</p>
          <button className="link" onClick={() => setOpen(open === p.id ? null : p.id)}>{open === p.id ? 'Hide' : 'Show'} replies</button>
          {open === p.id && <Thread post={p} />}
        </div>
      ))}
      {data?.length === 0 && <p className="muted">No discussions yet. Start one!</p>}
      <form className="card" onSubmit={post}>
        <h3>New discussion</h3>
        <input placeholder="Title" required value={f.title} onChange={e => setF({ ...f, title: e.target.value })} />
        <textarea placeholder="What's on your mind?" required value={f.body} onChange={e => setF({ ...f, body: e.target.value })} />
        <button>Post</button>
      </form>
    </div>
  );
}

/* ------------------------------ Progress / Students ------------------------------ */
function Progress({ course }: P) {
  const { data: prog, reload } = useFetch<{ percent: number }>(`/courses/${course.id}/progress`);
  const { data: certs, reload: reloadCerts } = useFetch<Certificate[]>('/certificates/mine');
  const [error, setError] = useState('');
  const cert = (certs ?? []).find(c => c.course.id === course.id);
  const percent = prog?.percent ?? 0;

  const claim = async () => {
    try { await api(`/certificates/course/${course.id}`, { method: 'POST' }); reloadCerts(); }
    catch (e) { setError((e as Error).message); }
  };
  return (
    <div className="card">
      <h3>Your progress</h3>
      <div className="bar"><div style={{ width: `${percent}%` }} /><span>{percent}%</span></div>
      <button className="link" onClick={reload}>Refresh</button>
      {error && <p className="error">{error}</p>}
      {cert ? (
        <p>
          Certificate earned!{' '}
          <button onClick={() => openFile(`/certificates/${cert.code}/pdf`).catch(e => alert((e as Error).message))}>View certificate</button>{' '}
          <button onClick={() => download(`/certificates/${cert.code}/pdf`, `certificate-${course.title}.pdf`)}>Download PDF</button>{' '}
          <small className="muted">Verify: /verify/{cert.code}</small>
        </p>
      ) : (
        <button disabled={percent < 100} onClick={claim}>{percent < 100 ? 'Finish the course to earn a certificate' : 'Get my certificate'}</button>
      )}
    </div>
  );
}

function Students({ course }: P) {
  const { data } = useFetch<StudentProgress[]>(`/courses/${course.id}/students`);
  const { data: pays } = useFetch<Payment[]>(course.priceCents > 0 ? `/payments/course/${course.id}` : null);
  return (
    <div>
      <div className="card">
        <h3>Enrolled students ({data?.length ?? 0})</h3>
        {(data ?? []).map(s => (
          <div key={s.student.id} className="row"><span>{s.student.name} <small className="muted">{s.student.email}</small></span><strong>{s.progress}%</strong></div>
        ))}
      </div>
      {pays && (
        <div className="card">
          <h3>Payments</h3>
          {pays.map(p => (
            <div key={p.id} className="row">
              <span>{p.student?.name} · {fmt(p.createdAt)}</span>
              <span>{money(p.amountCents)} <small className="badge">{p.status}</small></span>
            </div>
          ))}
          {pays.length === 0 && <p className="muted">No payments yet.</p>}
        </div>
      )}
    </div>
  );
}

/* ------------------------------ Page ------------------------------ */
export default function CoursePage() {
  const { id } = useParams();
  const { user } = useAuth();
  const { data: course, error } = useFetch<Course>(`/courses/${id}`);
  const isStudent = user?.role === 'STUDENT';
  const { data: enrolled, reload } = useFetch<EnrolledCourse[]>(isStudent ? '/enrollments/mine' : null);
  const [tab, setTab] = useState('materials');
  const [msg, setMsg] = useState('');

  if (error) return <p className="error">{error}</p>;
  if (!course || !user) return <p>Loading…</p>;

  const owner = user.role === 'ADMIN' || course.instructor.id === user.id;
  const isEnrolled = (enrolled ?? []).some(e => e.course.id === course.id);

  const enroll = async () => {
    setMsg('');
    try {
      if (course.priceCents > 0) {
        await payForCourse(course.id, user.name, user.email);   // Razorpay popup -> backend verifies -> enrolled
        reload();
      } else {
        await api(`/courses/${course.id}/enroll`, { method: 'POST' });
        reload();
      }
    } catch (e) { setMsg((e as Error).message); }
  };

  const tabs = ['materials', 'assignments', 'quizzes', 'forum', isStudent ? 'progress' : 'students'];
  const P: P = { course, owner, student: isStudent };

  return (
    <div>
      <div className="hero">
        <Thumb id={course.id} title={course.title} large />
        <div>
          <Link to="/" className="crumb">← Courses</Link>
          <h1>{course.title}</h1>
          <p className="lead">{course.description}</p>
          <p className="muted">By {course.instructor.name} · <strong>{money(course.priceCents)}</strong></p>
        </div>
      </div>

      {isStudent && !isEnrolled ? (
        <div className="card buy">
          <div>
            <h2>{course.priceCents > 0 ? money(course.priceCents) : 'Free'}</h2>
            <ul className="perks">
              {['Full lifetime access', 'Assignments, quizzes and discussions', 'Certificate of completion'].map(t => <li key={t}><Icon n="check" s={16} />{t}</li>)}
            </ul>
          </div>
          <div>
            {msg && <p className="error" role="alert">{msg}</p>}
            <button className="wide" onClick={enroll}>{course.priceCents > 0 ? 'Buy this course' : 'Enroll for free'}</button>
            {course.priceCents > 0 && <small className="muted">Secure checkout with Razorpay</small>}
          </div>
        </div>
      ) : (
        <>
          <div className="tabs">
            {tabs.map(t => <button key={t} className={t === tab ? 'active' : ''} onClick={() => setTab(t)}>{t}</button>)}
          </div>
          {tab === 'materials' && <Materials {...P} />}
          {tab === 'assignments' && <Assignments {...P} />}
          {tab === 'quizzes' && <Quizzes {...P} />}
          {tab === 'forum' && <Forum {...P} />}
          {tab === 'progress' && <Progress {...P} />}
          {tab === 'students' && <Students {...P} />}
        </>
      )}
    </div>
  );
}
