import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../useAuth'

export function ProtectedRoute() {
  const { user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return <main className="app-shell"><p>Restoring session...</p></main>
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
