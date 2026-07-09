import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { UploadCloud, Film, Loader2, Sparkles } from 'lucide-react';
import { api, apiErrorMessage } from '../api/client';
import type { OfferBreakdown, Video } from '../types';
import { formatBytes, formatDuration, formatMoney, formatNumber } from '../lib/format';

const FALLBACK_CATEGORIES = [
  'education', 'technology', 'finance', 'gaming', 'music', 'entertainment', 'vlog', 'sports', 'other',
];

export default function Upload() {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const [categories, setCategories] = useState<string[]>(FALLBACK_CATEGORIES);
  const [file, setFile] = useState<File | null>(null);
  const [duration, setDuration] = useState(0);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('education');
  const [expectedViews, setExpectedViews] = useState('');
  const [estimate, setEstimate] = useState<OfferBreakdown | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [progress, setProgress] = useState(0);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    api
      .get<{ categories: string[] }>('/meta/categories')
      .then(({ data }) => setCategories(data.categories))
      .catch(() => undefined);
  }, []);

  // Live estimate preview whenever the pricing inputs change.
  useEffect(() => {
    const params = new URLSearchParams({ category, durationSeconds: String(duration) });
    if (expectedViews) params.set('expectedMonthlyViews', expectedViews);
    const id = setTimeout(() => {
      api
        .get<OfferBreakdown>(`/videos/estimate?${params.toString()}`)
        .then(({ data }) => setEstimate(data))
        .catch(() => undefined);
    }, 250);
    return () => clearTimeout(id);
  }, [category, duration, expectedViews]);

  const handleFile = useCallback((f: File | undefined) => {
    if (!f) return;
    if (!f.type.startsWith('video/')) {
      setError('Please choose a video file.');
      return;
    }
    setError(null);
    setFile(f);
    if (!title) setTitle(f.name.replace(/\.[^.]+$/, ''));
    const el = document.createElement('video');
    el.preload = 'metadata';
    el.onloadedmetadata = () => {
      setDuration(Math.round(el.duration || 0));
      URL.revokeObjectURL(el.src);
    };
    el.src = URL.createObjectURL(f);
  }, [title]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!file) {
      setError('A video file is required.');
      return;
    }
    setError(null);
    setUploading(true);
    setProgress(0);
    try {
      const form = new FormData();
      form.append('file', file);
      form.append('title', title);
      form.append('description', description);
      form.append('category', category);
      form.append('durationSeconds', String(duration));
      if (expectedViews) form.append('expectedMonthlyViews', expectedViews);

      const { data } = await api.post<Video>('/videos', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (evt) => {
          if (evt.total) setProgress(Math.round((evt.loaded / evt.total) * 100));
        },
      });
      navigate(`/videos/${data.id}`);
    } catch (err) {
      setError(apiErrorMessage(err, 'Upload failed'));
      setUploading(false);
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
      <div>
        <h1 className="text-3xl font-bold">Upload a video</h1>
        <p className="mt-1 text-muted-foreground">
          We'll estimate its YouTube earning potential and make you an instant buyout offer.
        </p>

        {error && (
          <div role="alert" className="mt-4 rounded-xl bg-destructive/15 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        )}

        <form className="mt-6 space-y-5" onSubmit={onSubmit}>
          <div
            className={`card flex cursor-pointer flex-col items-center justify-center gap-3 border-2 border-dashed p-8 text-center transition-colors ${
              dragActive ? 'border-primary bg-primary/5' : 'border-border'
            }`}
            onClick={() => inputRef.current?.click()}
            onDragOver={(e) => {
              e.preventDefault();
              setDragActive(true);
            }}
            onDragLeave={() => setDragActive(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDragActive(false);
              handleFile(e.dataTransfer.files?.[0]);
            }}
          >
            <span className="grid h-14 w-14 place-items-center rounded-2xl bg-primary/15 text-primary">
              {file ? <Film size={26} /> : <UploadCloud size={26} />}
            </span>
            {file ? (
              <div>
                <div className="font-semibold">{file.name}</div>
                <div className="mt-0.5 text-sm text-muted-foreground">
                  {formatBytes(file.size)} · {formatDuration(duration)}
                </div>
              </div>
            ) : (
              <div>
                <div className="font-semibold">Drag & drop your video here</div>
                <div className="mt-0.5 text-sm text-muted-foreground">or click to browse (MP4, MOV, WebM…)</div>
              </div>
            )}
            <input
              ref={inputRef}
              type="file"
              accept="video/*"
              className="hidden"
              onChange={(e) => handleFile(e.target.files?.[0])}
            />
          </div>

          <div>
            <label className="label" htmlFor="title">Title *</label>
            <input id="title" className="input" value={title} onChange={(e) => setTitle(e.target.value)} required />
          </div>

          <div>
            <label className="label" htmlFor="description">Description</label>
            <textarea
              id="description"
              className="input min-h-[90px] resize-y"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="label" htmlFor="category">Category</label>
              <select
                id="category"
                className="input capitalize"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                {categories.map((c) => (
                  <option key={c} value={c} className="capitalize">
                    {c}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label" htmlFor="views">Expected monthly views</label>
              <input
                id="views"
                type="number"
                min={0}
                inputMode="numeric"
                placeholder="Auto-estimated"
                className="input"
                value={expectedViews}
                onChange={(e) => setExpectedViews(e.target.value)}
              />
              <p className="mt-1 text-xs text-muted-foreground">Leave blank to auto-estimate by category.</p>
            </div>
          </div>

          {uploading && (
            <div>
              <div className="mb-1 flex justify-between text-xs text-muted-foreground">
                <span>Uploading…</span>
                <span>{progress}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-white/10">
                <div className="h-full bg-primary transition-all" style={{ width: `${progress}%` }} />
              </div>
            </div>
          )}

          <button className="btn-primary w-full" type="submit" disabled={uploading || !file}>
            {uploading ? <Loader2 size={18} className="animate-spin" /> : <UploadCloud size={18} />}
            {uploading ? 'Uploading…' : 'Upload & get offer'}
          </button>
        </form>
      </div>

      <aside className="lg:sticky lg:top-24 lg:self-start">
        <div className="card overflow-hidden">
          <div className="flex items-center gap-2 border-b border-border bg-primary/10 px-5 py-4">
            <Sparkles size={18} className="text-primary" />
            <h2 className="font-semibold">Estimated offer</h2>
          </div>
          {estimate ? (
            <div className="p-5">
              <div className="font-mono text-4xl font-bold text-primary">
                {formatMoney(estimate.offerPrice, estimate.currency)}
              </div>
              <p className="mt-1 text-sm text-muted-foreground">
                Based on ~{formatNumber(estimate.estimatedMonthlyViews)} monthly views
              </p>
              <dl className="mt-5 space-y-2 text-sm">
                {[
                  ['Monthly revenue', formatMoney(estimate.estimatedMonthlyRevenue, estimate.currency)],
                  [`Projected (${estimate.projectionMonths} mo)`, formatMoney(estimate.projectedRevenue, estimate.currency)],
                  ['Buyout share', `${(estimate.buyoutShare * 100).toFixed(0)}%`],
                  ['Platform fee', `${(estimate.platformFee * 100).toFixed(0)}%`],
                ].map(([k, v]) => (
                  <div key={k} className="flex justify-between border-b border-border/60 pb-2">
                    <dt className="text-muted-foreground">{k}</dt>
                    <dd className="font-mono">{v}</dd>
                  </div>
                ))}
              </dl>
            </div>
          ) : (
            <div className="p-8 text-center text-sm text-muted-foreground">
              Add a file to preview your offer.
            </div>
          )}
        </div>
      </aside>
    </div>
  );
}
