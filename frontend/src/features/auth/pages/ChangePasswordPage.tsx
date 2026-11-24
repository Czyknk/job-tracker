import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../useAuth'

export function ChangePasswordPage() {
  const { changePassword } = useAuth()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')

    if (newPassword.length < 12) {
      setError('New password must be at least 12 characters.')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match.')
      return
    }

    setSubmitting(true)
    try {
      await changePassword({ currentPassword, newPassword })
    } catch (changeError) {
      setError(changeError instanceof ApiError
        ? changeError.problem.detail ?? changeError.problem.title
        : 'Unable to change the password.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="app-shell">
      <form className="login-card" onSubmit={handleSubmit}>
        <p className="eyebrow">Account</p>
        <h1>Change password</h1>
        {error && <p className="form-error" role="alert">{error}</p>}

        <label>
          Current password
          <input
            type="password"
            autoComplete="current-password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            required
          />
        </label>

        <label>
          New password
          <input
            type="password"
            autoComplete="new-password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            required
            minLength={12}
          />
        </label>

        <label>
          Confirm new password
          <input
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            required
            minLength={12}
          />
        </label>

        <button type="submit" disabled={submitting}>
          {submitting ? 'Changing...' : 'Change password'}
        </button>
        <Link className="back-link" to="/">Back to home</Link>
      </form>
    </main>
  )
}
