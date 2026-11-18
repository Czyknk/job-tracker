import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './features/auth/components/ProtectedRoute'
import { LoginPage } from './features/auth/pages/LoginPage'
import { useAuth } from './features/auth/useAuth'
import { CompaniesPage } from './features/companies/pages/CompaniesPage'
import { CompanyDetailsPage } from './features/companies/pages/CompanyDetailsPage'
import { CompanyFormPage } from './features/companies/pages/CompanyFormPage'

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

function AppLayout() {
  const { user, logout } = useAuth()

  return (
    <>
      <header className="app-header">
        <Link className="app-brand" to="/companies">Job Tracker</Link>
        <nav className="app-nav" aria-label="Main navigation">
          <Link to="/companies">Companies</Link>
        </nav>
        <div className="app-user">
          <span>{user?.email}</span>
          <button type="button" className="button-secondary" onClick={() => void logout()}>
            Sign out
          </button>
        </div>
      </header>
      <main className="page-container">
        <Routes>
          <Route path="/companies" element={<CompaniesPage />} />
          <Route path="/companies/new" element={<CompanyFormPage />} />
          <Route path="/companies/:companyId" element={<CompanyDetailsPage />} />
          <Route path="/companies/:companyId/edit" element={<CompanyFormPage />} />
          <Route path="*" element={<h1>Page not found</h1>} />
        </Routes>
      </main>
    </>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<Navigate to="/companies" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/*" element={<AppLayout />} />
      </Route>
      <Route path="*" element={<h1>Page not found</h1>} />
    </Routes>
  )
}

export default App
