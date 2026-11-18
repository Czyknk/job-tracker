import { useState, type FormEvent } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../useAuth'

export function LoginPage() {
  const { user, isLoading, login } = useAuth()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (!isLoading && user) {
    const destination = typeof location.state?.from === 'string'
      ? location.state.from
      : '/'
    return <Navigate to={destination} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      await login({ email, password })
    } catch (loginError) {
      if (loginError instanceof ApiError) {
        setError(loginError.problem.detail ?? loginError.problem.title)
      } else {
        setError('Unable to connect to the server.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="app-shell">
      <form className="login-card" onSubmit={handleSubmit}>
        <p className="eyebrow">Job Tracker</p>
        <h1>Sign in</h1>

        {error && <p className="form-error" role="alert">{error}</p>}

        <label>
          Email
          <input
            type="email"
            name="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            maxLength={255}
          />
        </label>

        <label>
          Password
          <input
            type="password"
            name="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            maxLength={72}
          />
        </label>

        <button type="submit" disabled={submitting}>
          {submitting ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </main>
  )
}
