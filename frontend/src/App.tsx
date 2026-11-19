import { Link, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './features/auth/components/ProtectedRoute'
import { LoginPage } from './features/auth/pages/LoginPage'
import { useAuth } from './features/auth/useAuth'
import { CompaniesPage } from './features/companies/pages/CompaniesPage'
import { CompanyDetailsPage } from './features/companies/pages/CompanyDetailsPage'
import { CompanyFormPage } from './features/companies/pages/CompanyFormPage'
import { ApplicationsPage } from './features/applications/pages/ApplicationsPage'
import { ApplicationDetailsPage } from './features/applications/pages/ApplicationDetailsPage'
import { ApplicationFormPage } from './features/applications/pages/ApplicationFormPage'
import { ContactsPage } from './features/contacts/pages/ContactsPage'
import { ContactDetailsPage } from './features/contacts/pages/ContactDetailsPage'
import { ContactFormPage } from './features/contacts/pages/ContactFormPage'
import { DocumentsPage } from './features/documents/pages/DocumentsPage'

function Dashboard() {
  const { accessToken, user, logout } = useAuth()

  async function handleLogout() {
    await logout()
  }

  return (
    <main className="app-shell">
      <section className="dashboard-card" aria-labelledby="welcome-title">
        <p className="eyebrow">Job Tracker</p>
        <h1 id="welcome-title">Welcome back</h1>

        <dl className="session-details">
          <div>
            <dt>Email</dt>
            <dd>{user?.email ?? 'Authenticated user'}</dd>
          </div>
          <div>
            <dt>Role</dt>
            <dd>{user?.role}</dd>
          </div>
        </dl>
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
        
        <div className="dashboard-actions" aria-label="Quick navigation">
          <Link className="dashboard-link" to="/applications">
            <strong>Applications</strong>
            <span>Review your job search pipeline</span>
          </Link>
          <Link className="dashboard-link" to="/companies">
            <strong>Companies</strong>
            <span>Manage companies and employers</span>
          </Link>
          <Link className="dashboard-link" to="/contacts">
            <strong>Contacts</strong>
            <span>Manage your professional network</span>
          </Link>
          <Link className="dashboard-link" to="/documents">
            <strong>Documents</strong>
            <span>Store and organize your important files</span>
          </Link>
        </div>
        
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
        <Link className="app-brand" to="/">Job Tracker</Link>
        <nav className="app-nav" aria-label="Main navigation">
          <Link to="/">Home</Link>
          <Link to="/companies">Companies</Link>
          <Link to="/applications">Applications</Link>
          <Link to="/contacts">Contacts</Link>
          <Link to="/documents">Documents</Link>
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
          <Route path="/applications" element={<ApplicationsPage />} />
          <Route path="/applications/new" element={<ApplicationFormPage />} />
          <Route path="/applications/:applicationId" element={<ApplicationDetailsPage />} />
          <Route path="/applications/:applicationId/edit" element={<ApplicationFormPage />} />
          <Route path="/contacts" element={<ContactsPage />} />
          <Route path="/contacts/new" element={<ContactFormPage />} />
          <Route path="/contacts/:contactId" element={<ContactDetailsPage />} />
          <Route path="/contacts/:contactId/edit" element={<ContactFormPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
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
        <Route path="/" element={<Dashboard />} />
        <Route path="/*" element={<AppLayout />} />
      </Route>
      <Route path="*" element={<h1>Page not found</h1>} />
    </Routes>
  )
}

export default App
