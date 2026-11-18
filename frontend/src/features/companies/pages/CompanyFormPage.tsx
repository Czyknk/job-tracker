import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { createCompany, getCompany, updateCompany } from '../companyApi'
import type { CompanyRequest, CompanyResponse } from '../companyTypes'
import { CompanyForm } from '../components/CompanyForm'

export function CompanyFormPage() {
  const { companyId } = useParams()
  const isEdit = companyId !== undefined
  const numericCompanyId = Number(companyId)
  const { accessToken, user } = useAuth()
  const navigate = useNavigate()
  const [company, setCompany] = useState<CompanyResponse | undefined>()
  const [loading, setLoading] = useState(isEdit)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const invalidCompanyId = isEdit && !Number.isInteger(numericCompanyId)

  useEffect(() => {
    if (!isEdit || !accessToken) return
    if (invalidCompanyId) return

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
  }, [accessToken, invalidCompanyId, isEdit, numericCompanyId])

  async function handleSubmit(request: CompanyRequest) {
    if (!accessToken) return

    setSubmitting(true)
    setError('')

    try {
      const saved = isEdit
        ? await updateCompany(numericCompanyId, request, accessToken)
        : await createCompany(request, accessToken)
      navigate(`/companies/${saved.id}`)
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to save the company.')
    } finally {
      setSubmitting(false)
    }
  }

  if (user?.role === 'DEMO') {
    return (
      <section className="content-card">
        <p className="eyebrow">Read-only demo</p>
        <h1>Editing is unavailable</h1>
        <p>DEMO accounts can view companies but cannot create or modify them.</p>
        <Link className="button-secondary" to="/companies">Back to companies</Link>
      </section>
    )
  }

  if (invalidCompanyId) {
    return (
      <section className="content-card">
        <p className="eyebrow">Companies</p>
        <h1>Invalid company</h1>
        <p>The company id in the address is not a valid number.</p>
        <Link className="button-secondary" to="/companies">Back to companies</Link>
      </section>
    )
  }

  return (
    <section className="content-card">
      <p className="eyebrow">Companies</p>
      <h1>{isEdit ? 'Edit company' : 'Add company'}</h1>

      {loading
        ? <p>Loading company...</p>
        : (
          <CompanyForm
            company={company}
            submitting={submitting}
            error={error}
            onSubmit={handleSubmit}
            onCancel={() => navigate(isEdit ? `/companies/${numericCompanyId}` : '/companies')}
            readOnly={false}
          />
        )}
    </section>
  )
}
