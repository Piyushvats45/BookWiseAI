import { useState, type FormEvent } from 'react'
import { useAuth } from '../auth'

export default function AuthPage() {
  const { login, register } = useAuth()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      if (mode === 'login') await login(email, password)
      else await register(displayName, email, password)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  const field =
    'mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-base outline-none focus:border-signal'

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
      <section className="flex flex-col justify-between bg-ink p-8 text-white sm:p-12">
        <p className="text-lg font-extrabold tracking-tight">
          Book<span className="text-ink mark">Wise</span>
        </p>

        <div className="my-12 max-w-xl">
          <h1 className="text-4xl font-bold leading-[1.1] tracking-tight sm:text-5xl">
            Ask your books.
            <br />
            Get answers with page numbers.
          </h1>
          <p className="mt-5 max-w-md text-lg text-white/75">
            Upload a textbook, paper or set of notes. BookWise answers from what&apos;s on the page and shows you
            where it found it.
          </p>
        </div>

        <figure className="max-w-xl rounded-lg bg-white p-5 text-ink" aria-label="Example answer">
          <p className="text-sm font-semibold text-slate">Where does the author define overfitting?</p>
          <p className="answer mt-2 text-base!">
            Overfitting is defined as a model that fits noise in the training data instead of the underlying pattern{' '}
            <span className="mark rounded px-1 font-sans text-sm font-semibold">1</span>.
          </p>
          <div className="mt-3 flex items-start gap-3 border-t border-line pt-3">
            <span className="shrink-0 rounded bg-ink px-2 py-0.5 text-xs font-semibold text-white">p. 112</span>
            <p className="answer text-sm! leading-relaxed!">
              <span className="mark">a model that has memorised the noise in its training set</span> will score well
              on that set and poorly on anything new…
            </p>
          </div>
        </figure>
      </section>

      <section className="flex items-center justify-center p-8 sm:p-12">
        <form onSubmit={submit} className="w-full max-w-sm">
          <h2 className="text-2xl font-bold tracking-tight">{mode === 'login' ? 'Sign in' : 'Create your account'}</h2>
          <p className="mt-1 text-slate">
            {mode === 'login' ? 'Welcome back to your library.' : 'Your books stay private to your account.'}
          </p>

          {mode === 'register' && (
            <label className="mt-6 block text-sm font-medium">
              Name
              <input
                className={field}
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                autoComplete="name"
                required
                maxLength={100}
              />
            </label>
          )}
          <label className="mt-4 block text-sm font-medium">
            Email
            <input
              className={field}
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              required
            />
          </label>
          <label className="mt-4 block text-sm font-medium">
            Password
            <input
              className={field}
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              required
              minLength={mode === 'register' ? 8 : undefined}
              maxLength={72}
            />
            {mode === 'register' && <span className="mt-1 block text-xs text-slate">Use at least 8 characters.</span>}
          </label>

          {error && (
            <p role="alert" className="mt-4 rounded-md bg-red-50 px-3 py-2 text-sm text-danger">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={busy}
            className="mt-6 w-full rounded-md bg-signal px-4 py-2.5 font-semibold text-white hover:bg-signal-dark disabled:opacity-60"
          >
            {busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>

          <p className="mt-5 text-sm text-slate">
            {mode === 'login' ? 'New to BookWise?' : 'Already have an account?'}{' '}
            <button
              type="button"
              className="font-semibold text-signal hover:underline"
              onClick={() => {
                setMode(mode === 'login' ? 'register' : 'login')
                setError(null)
              }}
            >
              {mode === 'login' ? 'Create an account' : 'Sign in'}
            </button>
          </p>
        </form>
      </section>
    </div>
  )
}
