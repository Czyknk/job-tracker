import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../useAuth'

export function ProtectedRoute() {
  const { user, isLoading, passwordChanged } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return <main className="app-shell"><p>Restoring session...</p></main>
  }

  if (!user) {
    const destination = passwordChanged ? '/' : location.pathname
    return <Navigate to="/login" replace state={{ from: destination }} />
  }

  return <Outlet />
}
