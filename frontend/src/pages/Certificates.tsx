import { useState } from 'react';
import { Link } from 'react-router-dom';
import { download, fmt, openFile, useFetch } from '../api';
import { Empty, Icon, Skeleton } from '../ui';
import type { Certificate } from '../types';

export default function Certificates() {
  const { data, error } = useFetch<Certificate[]>('/certificates/mine');
  const [copied, setCopied] = useState('');
  const copy = async (code: string) => {
    await navigator.clipboard.writeText(`${window.location.origin}/verify/${code}`);
    setCopied(code);
    setTimeout(() => setCopied(''), 1800);
  };

  return (
    <div>
      <h1>My certificates</h1>
      <p className="lead">Certificates for every course you have completed.</p>
      {error && <p className="error" role="alert">{error}</p>}
      {!data && !error ? <Skeleton n={2} /> : data && data.length === 0 ? (
        <Empty icon="award" title="No certificates yet" text="Finish 100% of a course (materials, assignments and quizzes) and your certificate appears here.">
          <Link to="/" className="btn">Go to my learning</Link>
        </Empty>
      ) : (
        <div className="grid">
          {(data ?? []).map(c => (
            <div key={c.id} className="card">
              <span className="fi"><Icon n="award" s={22} /></span>
              <h3>{c.course.title}</h3>
              <p className="muted">Issued {fmt(c.issuedAt)}</p>
              <button onClick={() => openFile(`/certificates/${c.code}/pdf`).catch(e => alert((e as Error).message))}>View certificate</button>
              <button className="ghostbtn" onClick={() => download(`/certificates/${c.code}/pdf`, `certificate-${c.course.title}.pdf`)}>Download PDF</button>
              <button className="link" onClick={() => copy(c.code)}>{copied === c.code ? 'Link copied' : 'Copy verification link'}</button>
              <p><Link to={`/verify/${c.code}`}>Open verification page</Link></p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
