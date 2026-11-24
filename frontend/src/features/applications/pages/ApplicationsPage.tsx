import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { listCompanies } from '../../companies/companyApi'
import type { CompanyResponse } from '../../companies/companyTypes'
import { listApplications } from '../applicationApi'
import type {
  ApplicationListItem,
  RecruitmentStatus,
} from '../applicationTypes'

const statuses: RecruitmentStatus[] = [
  'FOUND',
  'APPLIED',
  'HR_SCREEN',
  'TECH_TASK',
  'TECH_INTERVIEW',
  'FINAL_INTERVIEW',
  'OFFER_RECEIVED',
  'ACCEPTED',
  'REJECTED',
  'WITHDRAWN',
  'GHOSTED',
]

export function ApplicationsPage() {
  const { accessToken, user } = useAuth()
  const [applications, setApplications] = useState<ApplicationListItem[]>([])
  const [companies, setCompanies] = useState<CompanyResponse[]>([])
  const [status, setStatus] = useState<RecruitmentStatus | ''>('')
  const [companyId, setCompanyId] = useState<number | ''>('')
  const [query, setQuery] = useState('')
  const [archived, setArchived] = useState<'ACTIVE' | 'ARCHIVED' | 'ALL'>('ACTIVE')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accessToken) return

    let active = true

    Promise.all([
      listCompanies(accessToken),
      listApplications(accessToken, {
        status: status || undefined,
        companyId: companyId || undefined,
        q: query.trim() || undefined,
        archived,
        page,
        size: 20,
        sort: 'dateFound,desc',
      }),
    ])
      .then(([companyResult, applicationResult]) => {
        if (!active) return
        setCompanies(companyResult)
        setApplications(applicationResult.content)
        setTotalPages(applicationResult.totalPages)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to load applications.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [accessToken, archived, companyId, page, query, status])

  function changeFilter(action: () => void) {
    setPage(0)
    action()
  }

  return (
    <section className="content-card">
      <div className="page-header">
        <div>
          <p className="eyebrow">Applications</p>
          <h1>Job applications</h1>
        </div>
        {user?.role === 'ADMIN' && (
          <Link className="button-primary" to="/applications/new">
            Add application
          </Link>
        )}
      </div>

      <div className="application-filters">
        <label>
          Search
          <input
            value={query}
            onChange={(event) => changeFilter(() => setQuery(event.target.value))}
            placeholder="Position, company, location..."
          />
        </label>
        <label>
          Status
          <select
            value={status}
            onChange={(event) => changeFilter(() =>
              setStatus(event.target.value as RecruitmentStatus | ''),
            )}
          >
            <option value="">All statuses</option>
            {statuses.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>

        <label>
          Company
          <select
            value={companyId}
            onChange={(event) => changeFilter(() =>
              setCompanyId(event.target.value ? Number(event.target.value) : ''),
            )}
          >
            <option value="">All companies</option>
            {companies.map((company) => (
              <option key={company.id} value={company.id}>{company.name}</option>
            ))}
          </select>
        </label>

        <label>
          Archive state
          <select
            value={archived}
            onChange={(event) => changeFilter(() =>
              setArchived(event.target.value as 'ACTIVE' | 'ARCHIVED' | 'ALL'),
            )}
          >
            <option value="ACTIVE">Active</option>
            <option value="ARCHIVED">Archived</option>
            <option value="ALL">All</option>
          </select>
        </label>
      </div>

      {loading && <p>Loading applications...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {!loading && !error && applications.length === 0 && (
        <p>No applications match these filters.</p>
      )}

      {applications.length > 0 && (
        <div className="application-list">
          {applications.map((application) => (
            <Link
              key={application.id}
              className="application-row"
              to={`/applications/${application.id}`}
            >
              <div>
                <strong>{application.position}</strong>
                <span>{application.companyName}</span>
              </div>
              <div className="application-row-meta">
                <span className={`status-badge status-${application.currentStatus.toLowerCase()}`}>
                  {application.currentStatus}
                </span>
                <span>{application.dateFound}</span>
              </div>
            </Link>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button
            type="button"
            className="button-secondary"
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Previous
          </button>
          <span>Page {page + 1} of {totalPages}</span>
          <button
            type="button"
            className="button-secondary"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
          </button>
        </div>
      )}
    </section>
  )
}
