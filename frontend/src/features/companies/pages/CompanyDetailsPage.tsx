import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { deleteCompany, getCompany } from '../companyApi'
import type { CompanyResponse } from '../companyTypes'

export function CompanyDetailsPage() {
  const { companyId } = useParams()
  const numericCompanyId = Number(companyId)
  const { accessToken, user } = useAuth()
  const navigate = useNavigate()
  const [company, setCompany] = useState<CompanyResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accessToken || !Number.isInteger(numericCompanyId)) return

    let active = true
    getCompany(numericCompanyId, accessToken)
      .then((result) => {
        if (active) setCompany(result)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to load the company.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [accessToken, numericCompanyId])

  async function handleDelete() {
    if (!accessToken || !company) return
    if (!window.confirm(`Delete "${company.name}"?`)) return

    setDeleting(true)
    setError('')

    try {
      await deleteCompany(company.id, accessToken)
      navigate('/companies')
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to delete the company.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <section className="content-card">
      <Link className="back-link" to="/companies">Back to companies</Link>

      {loading && <p>Loading company...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {company && (
        <>
          <div className="page-header detail-header">
            <div>
              <p className="eyebrow">Company</p>
              <h1>{company.name}</h1>
            </div>
            {user?.role === 'ADMIN' && (
              <Link
                className="button-primary"
                to={`/companies/${company.id}/edit`}
              >
                Edit
              </Link>
            )}
          </div>

          <dl className="company-details">
            <div>
              <dt>Industry</dt>
              <dd>{company.industry || '-'}</dd>
            </div>
            <div>
              <dt>Website</dt>
              <dd>
                {company.website
                  ? <a href={company.website} target="_blank" rel="noreferrer">{company.website}</a>
                  : '-'}
              </dd>
            </div>
            <div>
              <dt>Notes</dt>
              <dd>{company.notes || '-'}</dd>
            </div>
          </dl>

          {user?.role === 'ADMIN' && (
            <button
              type="button"
              className="button-danger"
              onClick={handleDelete}
              disabled={deleting}
            >
              {deleting ? 'Deleting...' : 'Delete company'}
            </button>
          )}
        </>
      )}
    </section>
  )
}
