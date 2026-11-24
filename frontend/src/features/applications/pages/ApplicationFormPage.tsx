import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { listCompanies } from '../../companies/companyApi'
import type { CompanyResponse } from '../../companies/companyTypes'
import {
  createApplication,
  getApplication,
  updateApplication,
} from '../applicationApi'
import type { ApplicationRequest, ApplicationResponse } from '../applicationTypes'
import { ApplicationForm } from '../components/ApplicationForm'

export function ApplicationFormPage() {
  const { applicationId } = useParams()
  const isEdit = applicationId !== undefined
  const numericApplicationId = Number(applicationId)
  const invalidId = isEdit && !Number.isInteger(numericApplicationId)
  const { accessToken, user } = useAuth()
  const navigate = useNavigate()
  const [application, setApplication] = useState<ApplicationResponse | undefined>()
  const [companies, setCompanies] = useState<CompanyResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accessToken || invalidId) return

    let active = true
    const companyRequest = listCompanies(accessToken)
    const applicationRequest = isEdit
      ? getApplication(numericApplicationId, accessToken)
      : Promise.resolve<ApplicationResponse | undefined>(undefined)

    Promise.all([companyRequest, applicationRequest])
      .then(([companyResult, applicationResult]) => {
        if (!active) return
        setCompanies(companyResult)
        setApplication(applicationResult)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to load form data.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [accessToken, invalidId, isEdit, numericApplicationId])

  async function handleSubmit(request: ApplicationRequest) {
    if (!accessToken) return

    setSubmitting(true)
    setError('')

    try {
      const saved = isEdit
        ? await updateApplication(numericApplicationId, request, accessToken)
        : await createApplication(request, accessToken)
      navigate(`/applications/${saved.id}`)
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 409) {
        try {
          const refreshed = await getApplication(numericApplicationId, accessToken)
          setApplication(refreshed)
          setError('This application was changed elsewhere. It has been reloaded.')
        } catch {
          setError('This application was changed elsewhere, reload it before saving.')
        }
      } else {
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to save the application.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (user?.role === 'DEMO') {
    return (
      <section className="content-card">
        <p className="eyebrow">Read-only demo</p>
        <h1>Editing is unavailable</h1>
        <p>DEMO accounts can view applications but cannot create or modify them.</p>
        <Link className="button-secondary" to="/applications">Back to applications</Link>
      </section>
    )
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
      <p className="eyebrow">Applications</p>
      <h1>{isEdit ? 'Edit application' : 'Add application'}</h1>

      {loading
        ? <p>Loading form...</p>
        : companies.length === 0
          ? (
            <p>
              Create at least one company before adding an application.
              {' '}
              <Link to="/companies/new">Add company</Link>
            </p>
          )
          : (
            <ApplicationForm
              key={application ? `${application.id}-${application.version}` : 'new'}
              application={application}
              companies={companies}
              submitting={submitting}
              error={error}
              onSubmit={handleSubmit}
              onCancel={() => navigate(isEdit
                ? `/applications/${numericApplicationId}`
                : '/applications')}
              readOnly={false}
            />
          )}
    </section>
  )
}
