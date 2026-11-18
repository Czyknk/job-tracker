import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { deleteApplication, getApplication } from '../applicationApi'
import type { ApplicationResponse } from '../applicationTypes'

export function ApplicationDetailsPage() {
  const { applicationId } = useParams()
  const numericApplicationId = Number(applicationId)
  const invalidId = !Number.isInteger(numericApplicationId)
  const { accessToken, user } = useAuth()
  const navigate = useNavigate()
  const [application, setApplication] = useState<ApplicationResponse | null>(null)
  const [loading, setLoading] = useState(!invalidId)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accessToken || invalidId) return

    let active = true
    getApplication(numericApplicationId, accessToken)
      .then((result) => {
        if (active) setApplication(result)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to load the application.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [accessToken, invalidId, numericApplicationId])

  async function handleDelete() {
    if (!accessToken || !application) return
    if (!window.confirm(`Delete "${application.position}"?`)) return

    setDeleting(true)
    setError('')

    try {
      await deleteApplication(application.id, accessToken)
      navigate('/applications')
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to delete the application.')
    } finally {
      setDeleting(false)
    }
  }

  if (invalidId) {
    return (
      <section className="content-card">
        <h1>Invalid application</h1>
        <Link className="button-secondary" to="/applications">Back to applications</Link>
      </section>
    )
  }

  return (
    <section className="content-card">
      <Link className="back-link" to="/applications">Back to applications</Link>
      {loading && <p>Loading application...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {application && (
        <>
          <div className="page-header detail-header">
            <div>
              <p className="eyebrow">{application.companyName}</p>
              <h1>{application.position}</h1>
            </div>
            {user?.role === 'ADMIN' && (
              <Link
                className="button-primary"
                to={`/applications/${application.id}/edit`}
              >
                Edit
              </Link>
            )}
          </div>

          <span className={`status-badge status-${application.currentStatus.toLowerCase()}`}>
            {application.currentStatus}
          </span>

          <dl className="company-details application-details">
            <div><dt>Source</dt><dd>{application.source}</dd></div>
            <div><dt>Location</dt><dd>{application.location || '-'}</dd></div>
            <div><dt>Work mode</dt><dd>{application.workMode || '-'}</dd></div>
            <div><dt>Employment</dt><dd>{application.employmentType || '-'}</dd></div>
            <div><dt>Seniority</dt><dd>{application.seniorityLevel || '-'}</dd></div>
            <div>
              <dt>Offer URL</dt>
              <dd>
                {application.offerUrl
                  ? <a href={application.offerUrl} target="_blank" rel="noreferrer">{application.offerUrl}</a>
                  : '-'}
              </dd>
            </div>
            <div><dt>Date found</dt><dd>{application.dateFound}</dd></div>
            <div><dt>Date applied</dt><dd>{application.dateApplied || '-'}</dd></div>
            <div><dt>Deadline</dt><dd>{application.deadline || '-'}</dd></div>
            <div>
              <dt>Salary</dt>
              <dd>
                {application.salary
                  ? `${application.salary.min ?? '-'} - ${application.salary.max ?? '-'} ${application.salary.currency ?? ''} ${application.salary.period ?? ''} ${application.salary.basis ?? ''}`
                  : '-'}
              </dd>
            </div>
            <div><dt>Description</dt><dd>{application.rawDescription || '-'}</dd></div>
          </dl>

          {user?.role === 'ADMIN' && (
            <button
              type="button"
              className="button-danger"
              onClick={handleDelete}
              disabled={deleting}
            >
              {deleting ? 'Deleting...' : 'Delete application'}
            </button>
          )}
        </>
      )}
    </section>
  )
}
