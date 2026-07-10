import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
} from 'recharts';
import { ArrowLeft, Check, X, Loader2, Download, TrendingUp, Tag, Store } from 'lucide-react';
import { api, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { AiAnalysis, OfferBreakdown, Video } from '../types';
import { formatDate, formatDuration, formatMoney, formatNumber } from '../lib/format';
import StatusBadge from '../components/StatusBadge';
import AiAnalysisCard from '../components/AiAnalysisCard';

export default function VideoDetail() {
  const { id } = useParams<{ id: string }>();
  const { user, refreshUser } = useAuth();
  const [video, setVideo] = useState<Video | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [acting, setActing] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [listPrice, setListPrice] = useState('');

  async function load() {
    try {
      const { data } = await api.get<Video>(`/videos/${id}`);
      setVideo(data);
      if (data.aiFairPrice != null && !listPrice) setListPrice(String(data.aiFairPrice));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const isOwner = !!video && !!user && video.ownerId === user.id;

  const breakdown = useMemo<OfferBreakdown | null>(() => {
    if (!video?.offerBreakdown) return null;
    try {
      return JSON.parse(video.offerBreakdown) as OfferBreakdown;
    } catch {
      return null;
    }
  }, [video]);

  const aiAnalysis = useMemo<AiAnalysis | null>(() => {
    if (!video?.aiAnalysis) return null;
    try {
      return JSON.parse(video.aiAnalysis) as AiAnalysis;
    } catch {
      return null;
    }
  }, [video]);

  const chartData = breakdown
    ? [
        { name: 'Monthly', value: breakdown.estimatedMonthlyRevenue },
        { name: `Projected ${breakdown.projectionMonths}mo`, value: breakdown.projectedRevenue },
        { name: 'Gross offer', value: breakdown.grossOffer },
        { name: 'Your payout', value: breakdown.offerPrice },
      ]
    : [];

  async function act(action: 'accept-offer' | 'reject-offer') {
    if (!video) return;
    setActing(true);
    setError(null);
    try {
      const { data } = await api.post<Video>(`/videos/${video.id}/${action}`);
      setVideo(data);
      if (action === 'accept-offer') {
        await refreshUser();
        setNotice(`Offer accepted! ${formatMoney(data.offerPrice)} was credited to your wallet.`);
      } else {
        setNotice('Platform offer rejected.');
      }
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setActing(false);
    }
  }

  async function listForSale() {
    if (!video) return;
    setActing(true);
    setError(null);
    try {
      const { data } = await api.post<Video>(`/videos/${video.id}/list`, { price: Number(listPrice) });
      setVideo(data);
      setNotice(`Listed for sale at ${formatMoney(data.salePrice ?? 0)}. It's now on the marketplace.`);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setActing(false);
    }
  }

  async function unlist() {
    if (!video) return;
    setActing(true);
    setError(null);
    try {
      const { data } = await api.post<Video>(`/videos/${video.id}/unlist`);
      setVideo(data);
      setNotice('Removed from the marketplace.');
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setActing(false);
    }
  }

  async function download() {
    if (!video) return;
    try {
      const { data } = await api.get<{ url: string }>(`/videos/${video.id}/download-url`);
      window.open(data.url, '_blank', 'noopener');
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  }

  if (loading) return <div className="card h-64 animate-pulse" />;
  if (!video) {
    return (
      <div className="card p-8 text-center">
        <p className="text-muted-foreground">{error || 'Video not found.'}</p>
        <Link to="/videos" className="btn-ghost mt-4">Back to my videos</Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Link to="/videos" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft size={16} /> Back to my videos
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl font-bold">{video.title}</h1>
            <StatusBadge status={video.status} />
            {video.listedForSale && (
              <span className="badge bg-primary/20 text-primary">Listed {formatMoney(video.salePrice ?? 0)}</span>
            )}
          </div>
          <p className="mt-1 capitalize text-muted-foreground">
            {video.category} · {formatDuration(video.durationSeconds)} · by {video.ownerDisplayName} · {formatDate(video.createdAt)}
          </p>
        </div>
        {isOwner && (
          <button className="btn-ghost" onClick={download}>
            <Download size={16} /> Download
          </button>
        )}
      </div>

      {notice && (
        <div role="status" className="rounded-xl bg-success/15 px-4 py-3 text-sm text-success">{notice}</div>
      )}
      {error && (
        <div role="alert" className="rounded-xl bg-destructive/15 px-4 py-3 text-sm text-destructive">{error}</div>
      )}

      {video.description && (
        <div className="card p-5">
          <h2 className="mb-1 text-sm font-semibold uppercase tracking-wide text-muted-foreground">Description</h2>
          <p className="whitespace-pre-line text-sm">{video.description}</p>
        </div>
      )}

      {/* AI valuation */}
      {aiAnalysis && <AiAnalysisCard analysis={aiAnalysis} />}

      {/* Sell on the marketplace (owner only) */}
      {isOwner && video.status !== 'SOLD' && (
        <div className="card p-6">
          <div className="flex items-center gap-2 text-muted-foreground">
            <Store size={18} className="text-primary" />
            <span className="text-sm font-semibold uppercase tracking-wide">Sell on the marketplace</span>
          </div>
          {video.listedForSale ? (
            <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
              <p className="text-sm text-muted-foreground">
                Listed for <span className="font-mono font-semibold text-primary">{formatMoney(video.salePrice ?? 0)}</span>.
                Other users can buy it now.
              </p>
              <button className="btn-ghost" onClick={unlist} disabled={acting}>
                {acting ? <Loader2 size={16} className="animate-spin" /> : <X size={16} />} Remove listing
              </button>
            </div>
          ) : (
            <div className="mt-4 flex flex-wrap items-end gap-3">
              <div className="flex-1 min-w-[180px]">
                <label className="label" htmlFor="listPrice">Your price (USD)</label>
                <input
                  id="listPrice"
                  type="number"
                  min={0.01}
                  step="0.01"
                  className="input"
                  value={listPrice}
                  onChange={(e) => setListPrice(e.target.value)}
                  placeholder="e.g. 50.00"
                />
                {video.aiFairPrice != null && (
                  <p className="mt-1 text-xs text-muted-foreground">
                    AI suggests around {formatMoney(video.aiFairPrice)} as a fair price.
                  </p>
                )}
              </div>
              <button className="btn-primary" onClick={listForSale} disabled={acting || !listPrice}>
                {acting ? <Loader2 size={16} className="animate-spin" /> : <Tag size={16} />} List for sale
              </button>
            </div>
          )}
        </div>
      )}

      {/* Platform buyout offer */}
      <div className="grid gap-6 lg:grid-cols-[1fr_1.2fr]">
        <div className="card p-6">
          <div className="flex items-center gap-2 text-muted-foreground">
            <TrendingUp size={18} className="text-primary" />
            <span className="text-sm font-semibold uppercase tracking-wide">Platform buyout offer</span>
          </div>
          <div className="mt-3 font-mono text-5xl font-bold text-primary">
            {formatMoney(video.offerPrice)}
          </div>
          <p className="mt-2 text-sm text-muted-foreground">
            Estimated {formatNumber(video.estimatedMonthlyViews)} monthly views
          </p>

          {isOwner && video.status === 'OFFERED' ? (
            <div className="mt-6 flex flex-col gap-3 sm:flex-row">
              <button className="btn-primary flex-1" onClick={() => act('accept-offer')} disabled={acting}>
                {acting ? <Loader2 size={16} className="animate-spin" /> : <Check size={16} />}
                Accept offer
              </button>
              <button className="btn-ghost flex-1" onClick={() => act('reject-offer')} disabled={acting}>
                <X size={16} /> Reject
              </button>
            </div>
          ) : (
            <div className="mt-6 rounded-xl bg-white/5 px-4 py-3 text-sm text-muted-foreground">
              {video.status === 'SOLD'
                ? `Sold to the platform on ${formatDate(video.resolvedAt)} — payout credited.`
                : video.status === 'REJECTED'
                ? 'Platform buyout offer is closed.'
                : 'Platform buyout available to the owner.'}
            </div>
          )}
        </div>

        {breakdown && (
          <div className="card p-6">
            <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
              How the platform offer is calculated
            </h2>
            <div className="h-48">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ left: -18, right: 8, top: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
                  <XAxis dataKey="name" tick={{ fill: '#94A3B8', fontSize: 11 }} />
                  <YAxis tick={{ fill: '#94A3B8', fontSize: 11 }} />
                  <Tooltip
                    formatter={(v: number) => formatMoney(v, breakdown.currency)}
                    contentStyle={{
                      background: '#151C31',
                      border: '1px solid rgba(255,255,255,0.08)',
                      borderRadius: 12,
                      color: '#fff',
                    }}
                  />
                  <Bar dataKey="value" fill="#EC4899" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <ol className="mt-4 space-y-1.5 text-sm text-muted-foreground">
              {breakdown.steps.map((s, i) => (
                <li key={i} className="flex gap-2">
                  <span className="font-mono text-primary">{i + 1}.</span>
                  <span className="font-mono">{s}</span>
                </li>
              ))}
            </ol>
          </div>
        )}
      </div>
    </div>
  );
}
