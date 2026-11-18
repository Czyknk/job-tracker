import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './features/auth/components/ProtectedRoute'
import { LoginPage } from './features/auth/pages/LoginPage'
import { useAuth } from './features/auth/useAuth'

function Dashboard() {
  const { accessToken, user, logout } = useAuth()

  async function handleLogout() {
    await logout()
  }

  return (
    <main className="app-shell">
      <section className="welcome-card" aria-labelledby="welcome-title">
        <p className="eyebrow">Job Tracker</p>
        <h1 id="welcome-title">Welcome back</h1>
        <p className="welcome-text">
          Signed in as {user?.email ?? 'authenticated user'} ({user?.role}).
        </p>
        {accessToken && (
          <details className="token-details">
            <summary>Access token</summary>
            <textarea
              className="token-value"
              readOnly
              value={accessToken}
              aria-label="Access token"
              onFocus={(event) => event.currentTarget.select()}
            />
          </details>
        )}
        <button type="button" className="secondary-button" onClick={handleLogout}>
          Sign out
        </button>
      </section>
    </main>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<Dashboard />} />
      </Route>
      <Route path="*" element={<h1>Page not found</h1>} />
    </Routes>
  )
}

export default App
