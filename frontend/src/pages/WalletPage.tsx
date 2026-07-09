import { useEffect, useState } from 'react';
import { Wallet as WalletIcon, ArrowDownLeft, ArrowUpRight, Plus, Minus, Loader2 } from 'lucide-react';
import { api, apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { Wallet } from '../types';
import { formatDate, formatMoney } from '../lib/format';

export default function WalletPage() {
  const { refreshUser } = useAuth();
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState('50');
  const [busy, setBusy] = useState<'deposit' | 'withdraw' | null>(null);

  async function load() {
    try {
      const { data } = await api.get<Wallet>('/wallet');
      setWallet(data);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function move(kind: 'deposit' | 'withdraw') {
    setBusy(kind);
    setError(null);
    setNotice(null);
    try {
      const { data } = await api.post<Wallet>(`/wallet/${kind}`, { amount: Number(amount) });
      setWallet(data);
      await refreshUser();
      setNotice(
        kind === 'deposit'
          ? `Added ${formatMoney(Number(amount))} to your wallet.`
          : `Withdrew ${formatMoney(Number(amount))} from your wallet.`,
      );
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Wallet</h1>

      {notice && (
        <div role="status" className="rounded-xl bg-success/15 px-4 py-3 text-sm text-success">{notice}</div>
      )}
      {error && (
        <div role="alert" className="rounded-xl bg-destructive/15 px-4 py-3 text-sm text-destructive">{error}</div>
      )}

      <div className="grid gap-4 lg:grid-cols-[1fr_1.2fr]">
        <div className="card flex items-center justify-between overflow-hidden p-6">
          <div>
            <div className="text-sm text-muted-foreground">Available balance</div>
            <div className="mt-2 font-mono text-4xl font-bold text-primary">
              {formatMoney(wallet?.balance ?? 0)}
            </div>
          </div>
          <span className="grid h-16 w-16 place-items-center rounded-2xl bg-primary/15 text-primary">
            <WalletIcon size={28} />
          </span>
        </div>

        <div className="card p-6">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
            Add or withdraw funds
          </h2>
          <p className="mt-1 text-xs text-muted-foreground">
            Prototype only — no real payment processing. Funds are simulated.
          </p>
          <div className="mt-4 flex flex-wrap items-end gap-3">
            <div className="flex-1 min-w-[160px]">
              <label className="label" htmlFor="amount">Amount (USD)</label>
              <input
                id="amount"
                type="number"
                min={0.01}
                step="0.01"
                className="input"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>
            <button className="btn-primary" onClick={() => move('deposit')} disabled={busy !== null || !amount}>
              {busy === 'deposit' ? <Loader2 size={16} className="animate-spin" /> : <Plus size={16} />} Add funds
            </button>
            <button className="btn-ghost" onClick={() => move('withdraw')} disabled={busy !== null || !amount}>
              {busy === 'withdraw' ? <Loader2 size={16} className="animate-spin" /> : <Minus size={16} />} Withdraw
            </button>
          </div>
        </div>
      </div>

      <section>
        <h2 className="mb-3 text-xl font-semibold">Transaction history</h2>
        {loading ? (
          <div className="card h-32 animate-pulse" />
        ) : !wallet || wallet.transactions.length === 0 ? (
          <div className="card p-10 text-center text-muted-foreground">
            No transactions yet. Add funds or sell a video to get started.
          </div>
        ) : (
          <div className="card divide-y divide-border overflow-hidden">
            {wallet.transactions.map((tx) => {
              const positive = tx.amount >= 0;
              return (
                <div key={tx.id} className="flex items-center justify-between px-5 py-4">
                  <div className="flex items-center gap-3">
                    <span
                      className={`grid h-10 w-10 place-items-center rounded-xl ${
                        positive ? 'bg-success/15 text-success' : 'bg-destructive/15 text-destructive'
                      }`}
                    >
                      {positive ? <ArrowDownLeft size={18} /> : <ArrowUpRight size={18} />}
                    </span>
                    <div>
                      <div className="font-medium">{tx.description}</div>
                      <div className="text-xs text-muted-foreground">
                        {tx.type} · {formatDate(tx.createdAt)}
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className={`font-mono font-semibold ${positive ? 'text-success' : 'text-destructive'}`}>
                      {positive ? '+' : ''}{formatMoney(tx.amount)}
                    </div>
                    <div className="text-xs text-muted-foreground">bal {formatMoney(tx.balanceAfter)}</div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
