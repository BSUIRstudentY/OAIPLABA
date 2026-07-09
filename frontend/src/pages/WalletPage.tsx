import { useEffect, useState } from 'react';
import { Wallet as WalletIcon, ArrowDownLeft } from 'lucide-react';
import { api, apiErrorMessage } from '../api/client';
import type { Wallet } from '../types';
import { formatDate, formatMoney } from '../lib/format';

export default function WalletPage() {
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get<Wallet>('/wallet')
      .then(({ data }) => setWallet(data))
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Wallet</h1>

      {error && (
        <div role="alert" className="rounded-xl bg-destructive/15 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}

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

      <section>
        <h2 className="mb-3 text-xl font-semibold">Transaction history</h2>
        {loading ? (
          <div className="card h-32 animate-pulse" />
        ) : !wallet || wallet.transactions.length === 0 ? (
          <div className="card p-10 text-center text-muted-foreground">
            No transactions yet. Accept a buyout offer to get paid.
          </div>
        ) : (
          <div className="card divide-y divide-border overflow-hidden">
            {wallet.transactions.map((tx) => (
              <div key={tx.id} className="flex items-center justify-between px-5 py-4">
                <div className="flex items-center gap-3">
                  <span className="grid h-10 w-10 place-items-center rounded-xl bg-success/15 text-success">
                    <ArrowDownLeft size={18} />
                  </span>
                  <div>
                    <div className="font-medium">{tx.description}</div>
                    <div className="text-xs text-muted-foreground">
                      {tx.type} · {formatDate(tx.createdAt)}
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="font-mono font-semibold text-success">+{formatMoney(tx.amount)}</div>
                  <div className="text-xs text-muted-foreground">bal {formatMoney(tx.balanceAfter)}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
