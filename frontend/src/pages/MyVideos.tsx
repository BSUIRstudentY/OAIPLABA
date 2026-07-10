import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Clapperboard, UploadCloud } from 'lucide-react';
import { api, apiErrorMessage } from '../api/client';
import type { Video } from '../types';
import { formatDate, formatDuration, formatMoney } from '../lib/format';
import StatusBadge from '../components/StatusBadge';

export default function MyVideos() {
  const [videos, setVideos] = useState<Video[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get<Video[]>('/videos/mine')
      .then(({ data }) => setVideos(data))
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">My videos</h1>
        <Link to="/upload" className="btn-primary">
          <UploadCloud size={18} /> Upload
        </Link>
      </div>

      {error && (
        <div role="alert" className="rounded-xl bg-destructive/15 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}

      {loading ? (
        <div className="card h-40 animate-pulse" />
      ) : videos.length === 0 ? (
        <div className="card flex flex-col items-center gap-3 p-12 text-center">
          <Clapperboard size={32} className="text-muted-foreground" />
          <p className="text-muted-foreground">No videos yet.</p>
          <Link to="/upload" className="btn-primary">
            Upload your first video
          </Link>
        </div>
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-border text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th className="px-5 py-3">Title</th>
                <th className="px-5 py-3">Category</th>
                <th className="px-5 py-3">Length</th>
                <th className="px-5 py-3">Offer</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Uploaded</th>
              </tr>
            </thead>
            <tbody>
              {videos.map((v) => (
                <tr key={v.id} className="border-b border-border/50 last:border-0 hover:bg-white/5">
                  <td className="px-5 py-3">
                    <Link to={`/videos/${v.id}`} className="font-semibold text-foreground hover:text-primary">
                      {v.title}
                    </Link>
                  </td>
                  <td className="px-5 py-3 capitalize text-muted-foreground">{v.category}</td>
                  <td className="px-5 py-3 font-mono text-muted-foreground">{formatDuration(v.durationSeconds)}</td>
                  <td className="px-5 py-3 font-mono font-semibold text-primary">{formatMoney(v.offerPrice)}</td>
                  <td className="px-5 py-3"><StatusBadge status={v.status} /></td>
                  <td className="px-5 py-3 text-muted-foreground">{formatDate(v.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
