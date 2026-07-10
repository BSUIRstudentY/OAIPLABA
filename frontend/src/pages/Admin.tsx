import { useEffect, useState } from 'react';
import { Loader2, Save, Shield } from 'lucide-react';
import { api, apiErrorMessage } from '../api/client';
import type { PricingConfig, Video } from '../types';
import { formatDate, formatMoney } from '../lib/format';
import StatusBadge from '../components/StatusBadge';

const FIELDS: { key: keyof PricingConfig; label: string; step: string; hint: string }[] = [
  { key: 'averageCpm', label: 'Average CPM (USD / 1000 views)', step: '0.01', hint: 'Revenue per 1000 monetised views' },
  { key: 'watchTimeFactor', label: 'Watch-time factor (0–1)', step: '0.01', hint: 'Share of monetisable playbacks' },
  { key: 'projectionMonths', label: 'Projection months', step: '1', hint: 'Months of revenue paid for' },
  { key: 'buyoutShare', label: 'Buyout share (0–1)', step: '0.01', hint: 'Share of projected revenue offered' },
  { key: 'platformFee', label: 'Platform fee (0–1)', step: '0.01', hint: 'Fee withheld from the offer' },
  { key: 'baseMonthlyViews', label: 'Base monthly views', step: '100', hint: 'Baseline before category weighting' },
];

export default function Admin() {
  const [config, setConfig] = useState<PricingConfig | null>(null);
  const [videos, setVideos] = useState<Video[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.get<PricingConfig>('/admin/pricing-config').then(({ data }) => setConfig(data)).catch((e) => setError(apiErrorMessage(e)));
    api.get<Video[]>('/admin/videos').then(({ data }) => setVideos(data)).catch(() => undefined);
  }, []);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!config) return;
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const { data } = await api.put<PricingConfig>('/admin/pricing-config', config);
      setConfig(data);
      setNotice('Pricing configuration saved. New offers will use these values.');
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center gap-3">
        <span className="grid h-11 w-11 place-items-center rounded-xl bg-accent/20 text-accent">
          <Shield size={20} />
        </span>
        <div>
          <h1 className="text-3xl font-bold">Admin panel</h1>
          <p className="text-muted-foreground">Tune the buyout pricing model and review all uploads.</p>
        </div>
      </div>

      {error && (
        <div role="alert" className="rounded-xl bg-destructive/15 px-4 py-3 text-sm text-destructive">{error}</div>
      )}
      {notice && (
        <div role="status" className="rounded-xl bg-success/15 px-4 py-3 text-sm text-success">{notice}</div>
      )}

      <section className="card p-6">
        <h2 className="mb-4 text-xl font-semibold">Pricing model</h2>
        {config ? (
          <form className="grid gap-4 sm:grid-cols-2" onSubmit={save}>
            {FIELDS.map(({ key, label, step, hint }) => (
              <div key={key}>
                <label className="label" htmlFor={key}>{label}</label>
                <input
                  id={key}
                  type="number"
                  step={step}
                  className="input"
                  value={config[key] as number}
                  onChange={(e) => setConfig({ ...config, [key]: Number(e.target.value) })}
                  required
                />
                <p className="mt-1 text-xs text-muted-foreground">{hint}</p>
              </div>
            ))}
            <div className="sm:col-span-2">
              <button className="btn-primary" type="submit" disabled={saving}>
                {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
                Save configuration
              </button>
            </div>
          </form>
        ) : (
          <div className="h-40 animate-pulse rounded-xl bg-white/5" />
        )}
      </section>

      <section>
        <h2 className="mb-3 text-xl font-semibold">All videos ({videos.length})</h2>
        <div className="card overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-border text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th className="px-5 py-3">Title</th>
                <th className="px-5 py-3">Owner</th>
                <th className="px-5 py-3">Category</th>
                <th className="px-5 py-3">Offer</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Uploaded</th>
              </tr>
            </thead>
            <tbody>
              {videos.map((v) => (
                <tr key={v.id} className="border-b border-border/50 last:border-0 hover:bg-white/5">
                  <td className="px-5 py-3 font-medium">{v.title}</td>
                  <td className="px-5 py-3 text-muted-foreground">{v.ownerDisplayName}</td>
                  <td className="px-5 py-3 capitalize text-muted-foreground">{v.category}</td>
                  <td className="px-5 py-3 font-mono font-semibold text-primary">{formatMoney(v.offerPrice)}</td>
                  <td className="px-5 py-3"><StatusBadge status={v.status} /></td>
                  <td className="px-5 py-3 text-muted-foreground">{formatDate(v.createdAt)}</td>
                </tr>
              ))}
              {videos.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-5 py-10 text-center text-muted-foreground">No videos uploaded yet.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
