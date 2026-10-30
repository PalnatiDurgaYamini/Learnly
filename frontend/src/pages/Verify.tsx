import { useParams } from 'react-router-dom';
import { fmt, useFetch } from '../api';

interface Result { valid: boolean; student?: string; course?: string; issuedAt?: string }

export default function Verify() {
  const { code } = useParams();
  const { data } = useFetch<Result>(`/certificates/verify/${code}`);
  if (!data) return <p>Checking…</p>;
  return (
    <div className="card narrow">
      {data.valid
        ? <><h2>Valid certificate</h2><p><strong>{data.student}</strong> completed <strong>{data.course}</strong></p><p className="muted">Issued {fmt(data.issuedAt ?? null)}</p></>
        : <h2>Certificate not found</h2>}
    </div>
  );
}
