import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { UploadCloud, Clapperboard, Wallet, BadgeDollarSign, ArrowRight } from 'lucide-react';
import { api, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { Video } from '../types';
import { formatMoney } from '../lib/format';
import StatusBadge from '../components/StatusBadge';

export default function Dashboard() {
  const { user } = useAuth();
  const [videos, setVideos] = useState<Video[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const { data } = await api.get<Video[]>('/videos/mine');
        setVideos(data);
      } catch (err) {
        setError(apiErrorMessage(err));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const openOffers = videos.filter((v) => v.status === 'OFFERED');
  const sold = videos.filter((v) => v.status === 'SOLD');
  const potential = openOffers.reduce((sum, v) => sum + v.offerPrice, 0);

  const stats = [
    { label: 'Wallet balance', value: formatMoney(user?.walletBalance ?? 0), icon: Wallet, tone: 'text-primary' },
    { label: 'Open offers', value: String(openOffers.length), icon: BadgeDollarSign, tone: 'text-accent' },
    { label: 'Videos sold', value: String(sold.length), icon: Clapperboard, tone: 'text-success' },
    { label: 'Potential earnings', value: formatMoney(potential), icon: BadgeDollarSign, tone: 'text-primary' },
  ];

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">Hi, {user?.displayName} 👋</h1>
          <p className="mt-1 text-muted-foreground">Here's what's happening with your videos.</p>
        </div>
        <Link to="/upload" className="btn-primary">
          <UploadCloud size={18} /> Upload video
        </Link>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map(({ label, value, icon: Icon, tone }) => (
          <div key={label} className="card p-5">
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">{label}</span>
              <Icon size={18} className={tone} aria-hidden />
            </div>
            <div className={`mt-3 font-mono text-2xl font-bold ${tone}`}>{value}</div>
          </div>
        ))}
      </div>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-xl font-semibold">Recent uploads</h2>
          <Link to="/videos" className="text-sm font-medium text-primary hover:underline">
            View all
          </Link>
        </div>

        {error && (
          <div role="alert" className="rounded-xl bg-destructive/15 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        )}

        {loading ? (
          <div className="card h-24 animate-pulse" />
        ) : videos.length === 0 ? (
          <div className="card flex flex-col items-center gap-3 p-10 text-center">
            <Clapperboard size={32} className="text-muted-foreground" />
            <p className="text-muted-foreground">You haven't uploaded any videos yet.</p>
            <Link to="/upload" className="btn-primary">
              Upload your first video <ArrowRight size={16} />
            </Link>
          </div>
        ) : (
          <div className="grid gap-3">
            {videos.slice(0, 4).map((v) => (
              <Link
                key={v.id}
                to={`/videos/${v.id}`}
                className="card flex items-center justify-between p-4 transition-transform hover:-translate-y-0.5"
              >
                <div className="min-w-0">
                  <div className="truncate font-semibold">{v.title}</div>
                  <div className="mt-0.5 text-xs capitalize text-muted-foreground">{v.category}</div>
                </div>
                <div className="flex items-center gap-4">
                  <span className="font-mono font-bold text-primary">{formatMoney(v.offerPrice)}</span>
                  <StatusBadge status={v.status} />
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
