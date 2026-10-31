import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth'

export default function Shell({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth()
  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-line bg-white">
        <div className="mx-auto flex h-14 max-w-4xl items-center justify-between px-5">
          <Link to="/" className="text-lg font-extrabold tracking-tight">
            Book<span className="mark">Wise</span>
          </Link>
          <div className="flex items-center gap-4 text-sm">
            <span className="hidden text-slate sm:inline">{user?.displayName}</span>
            <button onClick={logout} className="rounded px-2 py-1 font-medium text-signal hover:bg-paper">
              Sign out
            </button>
          </div>
        </div>
      </header>
      <main className="flex min-h-0 flex-1 flex-col">{children}</main>
    </div>
  )
}
