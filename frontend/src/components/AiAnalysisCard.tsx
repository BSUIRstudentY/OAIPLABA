import { Sparkles, Clock, Plus, Minus, Equal, Cpu } from 'lucide-react';
import type { AiAnalysis } from '../types';
import { formatMoney } from '../lib/format';

function ImpactIcon({ impact }: { impact: string }) {
  if (impact === '+') return <Plus size={14} className="text-success" aria-label="positive factor" />;
  if (impact === '-') return <Minus size={14} className="text-destructive" aria-label="negative factor" />;
  if (impact === 'anchor') return <Cpu size={14} className="text-accent" aria-label="anchor factor" />;
  return <Equal size={14} className="text-muted-foreground" aria-label="neutral factor" />;
}

function recommendationClass(rec: string): string {
  if (rec === 'Worth buying') return 'bg-success/20 text-success';
  if (rec === 'Low value') return 'bg-primary/20 text-primary';
  return 'bg-destructive/20 text-destructive';
}

export default function AiAnalysisCard({ analysis }: { analysis: AiAnalysis }) {
  const t = analysis.technical;
  return (
    <div className="card overflow-hidden">
      <div className="flex items-center justify-between gap-2 border-b border-border bg-accent/10 px-5 py-4">
        <div className="flex items-center gap-2">
          <Sparkles size={18} className="text-accent" />
          <h2 className="font-semibold">AI valuation</h2>
        </div>
        <span className="badge bg-accent/20 text-accent">
          {Math.round(analysis.confidence * 100)}% confidence
        </span>
      </div>

      <div className="p-5">
        <div className="flex items-end justify-between gap-3">
          <div>
            <div className="text-xs uppercase tracking-wide text-muted-foreground">AI fair price</div>
            <div className="font-mono text-4xl font-bold text-accent">
              {formatMoney(analysis.fairPrice, analysis.currency)}
            </div>
            {analysis.recommendation && (
              <span className={`badge mt-2 ${recommendationClass(analysis.recommendation)}`}>
                {analysis.recommendation}
              </span>
            )}
          </div>
          <div className="text-right text-xs text-muted-foreground">
            {t.resolutionLabel} · {t.hasAudio ? 'audio' : 'silent'}
            <br />
            {t.durationSeconds > 0 && `${Math.floor(t.durationSeconds / 60)}:${String(t.durationSeconds % 60).padStart(2, '0')}`}
            {t.sceneChanges > 0 && ` · ${t.sceneChanges} cuts`}
          </div>
        </div>

        {typeof analysis.contentScore === 'number' && (
          <div className="mt-4">
            <div className="mb-1 flex justify-between text-xs text-muted-foreground">
              <span>Content value</span>
              <span className="font-mono">{Math.round(analysis.contentScore * 100)}%</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-white/10">
              <div className="h-full bg-accent transition-all" style={{ width: `${Math.round(analysis.contentScore * 100)}%` }} />
            </div>
          </div>
        )}

        <p className="mt-4 text-sm text-muted-foreground">{analysis.summary}</p>

        {analysis.contextTags?.length > 0 && (
          <div className="mt-4 flex flex-wrap gap-2">
            {analysis.contextTags.map((tag) => (
              <span key={tag} className="badge bg-white/5 capitalize text-muted-foreground">
                {tag}
              </span>
            ))}
          </div>
        )}

        <h3 className="mt-6 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          Why this price
        </h3>
        <ul className="mt-2 space-y-2">
          {analysis.factors.map((f, i) => (
            <li key={i} className="flex gap-3 text-sm">
              <span className="mt-0.5 shrink-0">
                <ImpactIcon impact={f.impact} />
              </span>
              <span>
                <span className="font-semibold">{f.name}: </span>
                <span className="text-muted-foreground">{f.detail}</span>
              </span>
            </li>
          ))}
        </ul>

        {analysis.keyMoments?.length > 0 && (
          <>
            <h3 className="mt-6 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
              Key moments
            </h3>
            <ul className="mt-2 flex flex-wrap gap-2">
              {analysis.keyMoments.map((m, i) => (
                <li
                  key={i}
                  className="flex items-center gap-2 rounded-lg border border-border bg-background/60 px-3 py-1.5 text-sm"
                >
                  <Clock size={14} className="text-accent" />
                  <span className="font-mono text-accent">{m.timecode}</span>
                  <span className="text-muted-foreground">{m.label}</span>
                </li>
              ))}
            </ul>
          </>
        )}

        <p className="mt-5 text-xs text-muted-foreground">Engine: {analysis.engine}</p>
      </div>
    </div>
  );
}
