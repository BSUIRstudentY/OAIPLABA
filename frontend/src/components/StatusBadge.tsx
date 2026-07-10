import type { VideoStatus } from '../types';

const MAP: Record<VideoStatus, { label: string; className: string }> = {
  OFFERED: { label: 'Offer open', className: 'bg-accent/20 text-accent' },
  SOLD: { label: 'Sold', className: 'bg-success/20 text-success' },
  REJECTED: { label: 'Rejected', className: 'bg-white/10 text-muted-foreground' },
};

export default function StatusBadge({ status }: { status: VideoStatus }) {
  const cfg = MAP[status];
  return <span className={`badge ${cfg.className}`}>{cfg.label}</span>;
}
