import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { listCompanies } from '../companyApi'
import type { CompanyResponse } from '../companyTypes'

export function CompaniesPage() {
  const { accessToken, user } = useAuth()
  const [companies, setCompanies] = useState<CompanyResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accessToken) return

    let active = true
    listCompanies(accessToken)
      .then((result) => {
        if (active) setCompanies(result)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to load companies.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [accessToken])

  return (
    <section className="content-card">
      <div className="page-header">
        <div>
          <p className="eyebrow">Companies</p>
          <h1>Company directory</h1>
        </div>
        {user?.role === 'ADMIN' && (
          <Link className="button-primary" to="/companies/new">
            Add company
          </Link>
        )}
      </div>

      {loading && <p>Loading companies...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {!loading && !error && companies.length === 0 && (
        <p>No companies have been added yet.</p>
      )}

      {companies.length > 0 && (
        <div className="company-list">
          {companies.map((company) => (
            <Link
              className="company-row"
              key={company.id}
              to={`/companies/${company.id}`}
            >
              <div>
                <strong>{company.name}</strong>
                {company.industry && <span>{company.industry}</span>}
              </div>
              <span aria-hidden="true">View</span>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}
