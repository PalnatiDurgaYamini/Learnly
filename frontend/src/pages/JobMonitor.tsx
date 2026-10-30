import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { WS_URL, api, getToken } from '../api';
import { Empty } from '../ui';
import type { Job } from '../types';

/** Spring may send LocalDateTime as an ISO string (REST) or as [y,m,d,h,mi,s,...] (WebSocket). */
const when = (v: Job['updatedAt']) => {
  if (!v) return '—';
  const d = Array.isArray(v) ? new Date(v[0], v[1] - 1, v[2], v[3] ?? 0, v[4] ?? 0, v[5] ?? 0) : new Date(v);
  return d.toLocaleTimeString();
};

/**
 * Admin-only: loads job history over REST, then listens on /topic/jobs (WebSocket + STOMP)
 * so statuses change PENDING -> PROCESSING -> COMPLETED/FAILED live, without a page refresh.
 */
export default function JobMonitor() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [live, setLive] = useState(false);

  useEffect(() => {
    api<Job[]>('/jobs').then(setJobs).catch(console.error);
    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: { Authorization: `Bearer ${getToken() ?? ''}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setLive(true);
        client.subscribe('/topic/jobs', msg => {
          const j: Job = JSON.parse(msg.body);
          setJobs(old => (old.some(x => x.jobId === j.jobId) ? old.map(x => (x.jobId === j.jobId ? j : x)) : [j, ...old]));
        });
      },
      onWebSocketClose: () => setLive(false),
      onStompError: () => setLive(false),
    });
    client.activate();
    return () => { void client.deactivate(); };
  }, []);

  return (
    <div>
      <p className="muted"><span className={`dot ${live ? 'on' : ''}`} />{live ? 'Live updates connected' : 'Connecting to live updates…'}</p>
      {jobs.length === 0 ? <Empty icon="chart" title="No background jobs yet" text="Email, invoice and notification jobs appear here after a student pays for a course." /> : (
        <div className="tablewrap job-monitor"><table className="tbl">
          <thead><tr><th>Job</th><th>Payment</th><th>Task</th><th>Student</th><th>Course</th><th>Status</th><th>Message</th><th>Updated</th></tr></thead>
          <tbody>{jobs.map(j => (
            <tr key={j.jobId}><td>{j.jobId}</td><td>#{j.paymentId}</td><td>{j.jobType}</td><td>{j.studentEmail}</td><td>{j.courseTitle}</td>
              <td><span className={`jobstatus ${j.status.toLowerCase()}`}>{j.status}</span></td><td>{j.message}</td><td>{when(j.updatedAt)}</td></tr>
          ))}</tbody>
        </table></div>
      )}
    </div>
  );
}
