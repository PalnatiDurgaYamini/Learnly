import { Link } from 'react-router-dom';
import { Icon } from '../ui';

const features = [
  { icon: 'book', t: 'Learn at your pace', d: 'Lessons, assignments and quizzes in one place, available whenever you are.' },
  { icon: 'chart', t: 'See your progress', d: 'Every completed lesson moves your progress bar, so you always know what is next.' },
  { icon: 'award', t: 'Earn certificates', d: 'Finish a course and download a verifiable PDF certificate.' },
  { icon: 'users', t: 'Learn together', d: 'Ask questions and share ideas in each course discussion forum.' },
];
const steps = ['Create a free account', 'Enroll in a course or publish your own', 'Complete lessons and earn your certificate'];

export default function Welcome() {
  return (
    <div className="landing">
      <section className="lhero">
        <span className="eyebrow">Online learning, simplified</span>
        <h1>Learn new skills.<br />Teach what you know.</h1>
        <p className="lead">Learnly brings courses, assignments, quizzes and certificates together in one simple platform for students and instructors.</p>
        <div className="cta">
          <Link to="/register" className="btn big">Get started free</Link>
          <Link to="/login" className="btn big outline">I already have an account</Link>
        </div>
        <ul className="trust">{['Free and paid courses', 'Verified certificates', 'Works on any device'].map(t => <li key={t}><Icon n="check" s={16} />{t}</li>)}</ul>
      </section>
      <section aria-labelledby="f">
        <h2 id="f" className="center">Everything you need to learn and teach</h2>
        <div className="features">{features.map(f => (
          <div key={f.t} className="feature"><span className="fi"><Icon n={f.icon} s={22} /></span><h3>{f.t}</h3><p className="muted">{f.d}</p></div>
        ))}</div>
      </section>
      <section aria-labelledby="h">
        <h2 id="h" className="center">How it works</h2>
        <ol className="steps">{steps.map((s, i) => <li key={s}><span className="n">{i + 1}</span>{s}</li>)}</ol>
      </section>
      <section className="band">
        <h2>Ready to start?</h2><p>Join as a student or instructor in under a minute.</p>
        <Link to="/register" className="btn big light">Create your account</Link>
      </section>
    </div>
  );
}
