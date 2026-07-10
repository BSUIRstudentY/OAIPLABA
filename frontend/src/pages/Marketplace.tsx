import { useEffect, useState } from 'react';
import { Store, ShoppingCart, Loader2, Sparkles, Clock } from 'lucide-react';
import { api, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { Video } from '../types';
import { formatDuration, formatMoney } from '../lib/format';

export default function Marketplace() {
  const { user, refreshUser } = useAuth();
  const [videos, setVideos] = useState<Video[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [buyingId, setBuyingId] = useState<string | null>(null);

  async function load() {
    try {
      const { data } = await api.get<Video[]>('/marketplace');
      setVideos(data);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function buy(v: Video) {
    setBuyingId(v.id);
    setError(null);
    setNotice(null);
    try {
      await api.post(`/marketplace/${v.id}/buy`);
      await refreshUser();
      setNotice(`You bought "${v.title}" for ${formatMoney(v.salePrice ?? 0)}. It's now in your videos.`);
      setVideos((prev) => prev.filter((x) => x.id !== v.id));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBuyingId(null);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="grid h-11 w-11 place-items-center rounded-xl bg-primary/15 text-primary">
            <Store size={20} />
          </span>
          <div>
            <h1 className="text-3xl font-bold">Marketplace</h1>
            <p className="text-muted-foreground">Buy videos listed by other creators at their price.</p>
          </div>
        </div>
        <div className="text-right text-sm">
          <div className="text-muted-foreground">Your balance</div>
          <div className="font-mono text-lg font-bold text-primary">{formatMoney(user?.walletBalance ?? 0)}</div>
        </div>
      </div>

      {notice && (
        <div role="status" className="rounded-xl bg-success/15 px-4 py-3 text-sm text-success">{notice}</div>
      )}
      {error && (
        <div role="alert" className="rounded-xl bg-destructive/15 px-4 py-3 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <div key={i} className="card h-56 animate-pulse" />
          ))}
        </div>
      ) : videos.length === 0 ? (
        <div className="card flex flex-col items-center gap-3 p-12 text-center">
          <Store size={32} className="text-muted-foreground" />
          <p className="text-muted-foreground">No videos are listed for sale right now.</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {videos.map((v) => {
            const affordable = (user?.walletBalance ?? 0) >= (v.salePrice ?? 0);
            return (
              <div key={v.id} className="card flex flex-col p-5">
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold leading-tight">{v.title}</h3>
                  <span className="badge shrink-0 bg-white/5 capitalize text-muted-foreground">{v.category}</span>
                </div>
                <div className="mt-1 text-xs text-muted-foreground">
                  by {v.ownerDisplayName} · {formatDuration(v.durationSeconds)}
                </div>
                {v.description && (
                  <p className="mt-3 line-clamp-2 text-sm text-muted-foreground">{v.description}</p>
                )}

                {v.aiFairPrice != null && (
                  <div className="mt-3 flex items-center gap-1.5 text-xs text-accent">
                    <Sparkles size={13} /> AI fair value {formatMoney(v.aiFairPrice)}
                  </div>
                )}

                <div className="mt-auto flex items-end justify-between gap-2 pt-4">
                  <div>
                    <div className="text-xs text-muted-foreground">Asking price</div>
                    <div className="font-mono text-2xl font-bold text-primary">{formatMoney(v.salePrice ?? 0)}</div>
                  </div>
                  <button
                    className="btn-primary"
                    onClick={() => buy(v)}
                    disabled={buyingId === v.id || !affordable}
                    title={affordable ? undefined : 'Insufficient balance — add funds in your wallet'}
                  >
                    {buyingId === v.id ? (
                      <Loader2 size={16} className="animate-spin" />
                    ) : (
                      <ShoppingCart size={16} />
                    )}
                    {affordable ? 'Buy' : 'Low balance'}
                  </button>
                </div>
                {v.durationSeconds > 0 && (
                  <div className="mt-2 flex items-center gap-1 text-[11px] text-muted-foreground">
                    <Clock size={11} /> {formatDuration(v.durationSeconds)} runtime
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
