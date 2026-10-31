import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './auth'
import Shell from './components/Shell'
import AuthPage from './pages/AuthPage'
import ChatPage from './pages/ChatPage'
import LibraryPage from './pages/LibraryPage'

function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <p className="p-8 text-slate">Loading…</p>
  if (!user) return <Navigate to="/login" replace />
  return <Shell>{children}</Shell>
}

export default function App() {
  const { user, loading } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={!loading && user ? <Navigate to="/" replace /> : <AuthPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <LibraryPage />
          </RequireAuth>
        }
      />
      <Route
        path="/books/:bookId"
        element={
          <RequireAuth>
            <ChatPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
