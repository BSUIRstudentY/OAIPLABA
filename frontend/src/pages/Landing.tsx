import { Link } from 'react-router-dom';
import { Vault, TrendingUp, ShieldCheck, Wallet, ArrowRight } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';

export default function Landing() {
  const { user } = useAuth();
  const primaryCta = user ? '/dashboard' : '/register';

  return (
    <div className="min-h-dvh">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5">
        <div className="flex items-center gap-2 font-mono text-lg font-bold">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary text-primary-foreground shadow-glow">
            <Vault size={18} />
          </span>
          Vid<span className="text-primary">Vault</span>
        </div>
        <div className="flex items-center gap-2">
          {user ? (
            <Link to="/dashboard" className="btn-primary">
              Open dashboard
            </Link>
          ) : (
            <>
              <Link to="/login" className="btn-ghost">
                Log in
              </Link>
              <Link to="/register" className="btn-primary">
                Get started
              </Link>
            </>
          )}
        </div>
      </header>

      <section className="mx-auto max-w-6xl px-4 pb-16 pt-10 text-center sm:pt-20">
        <span className="badge mx-auto mb-6 bg-primary/15 text-primary">
          <TrendingUp size={14} /> Instant buyout offers
        </span>
        <h1 className="mx-auto max-w-3xl text-4xl font-bold leading-tight sm:text-6xl">
          Sell your videos for their{' '}
          <span className="bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
            YouTube earning potential
          </span>
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground">
          Upload a video and VidVault instantly estimates how much it could earn through YouTube
          monetisation — then offers to buy it outright. Accept, and the payout lands in your wallet.
        </p>
        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
          <Link to={primaryCta} className="btn-primary text-base">
            Upload your first video <ArrowRight size={18} />
          </Link>
          <Link to="/login" className="btn-ghost text-base">
            I already have an account
          </Link>
        </div>
      </section>

      <section className="mx-auto grid max-w-6xl gap-4 px-4 pb-24 sm:grid-cols-3">
        {[
          {
            icon: TrendingUp,
            title: 'Transparent pricing',
            body: 'See exactly how projected monthly views, CPM and watch-time turn into your offer.',
          },
          {
            icon: ShieldCheck,
            title: 'Secure & authenticated',
            body: 'JWT-based auth, encrypted passwords and private, presigned video downloads.',
          },
          {
            icon: Wallet,
            title: 'Instant payouts',
            body: 'Accept an offer and the buyout amount is credited to your in-app wallet immediately.',
          },
        ].map(({ icon: Icon, title, body }) => (
          <div key={title} className="card p-6 text-left">
            <span className="mb-4 grid h-11 w-11 place-items-center rounded-xl bg-accent/15 text-accent">
              <Icon size={20} />
            </span>
            <h3 className="text-lg font-semibold">{title}</h3>
            <p className="mt-2 text-sm text-muted-foreground">{body}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
