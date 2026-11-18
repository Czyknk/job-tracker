import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { listCompanies } from '../../companies/companyApi'
import type { CompanyResponse } from '../../companies/companyTypes'
import { createContact, getContact, updateContact } from '../contactApi'
import type { ContactRequest, ContactResponse } from '../contactTypes'
import { ContactForm } from '../components/ContactForm'

export function ContactFormPage() {
  const { contactId } = useParams()
  const isEdit = contactId !== undefined
  const numericContactId = Number(contactId)
  const invalidId = isEdit && !Number.isInteger(numericContactId)
  const { accessToken, user } = useAuth()
  const navigate = useNavigate()
  const [contact, setContact] = useState<ContactResponse | undefined>()
  const [companies, setCompanies] = useState<CompanyResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accessToken || invalidId) return

    let active = true
    const contactRequest = isEdit
      ? getContact(numericContactId, accessToken)
      : Promise.resolve<ContactResponse | undefined>(undefined)

    Promise.all([listCompanies(accessToken), contactRequest])
      .then(([companyResult, contactResult]) => {
        if (!active) return
        setCompanies(companyResult)
        setContact(contactResult)
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
  }, [accessToken, invalidId, isEdit, numericContactId])

  async function handleSubmit(request: ContactRequest) {
    if (!accessToken) return
    setSubmitting(true)
    setError('')

    try {
      const saved = isEdit
        ? await updateContact(numericContactId, request, accessToken)
        : await createContact(request, accessToken)
      navigate(`/contacts/${saved.id}`)
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to save the contact.')
    } finally {
      setSubmitting(false)
    }
  }

  if (user?.role === 'DEMO') {
    return (
      <section className="content-card">
        <p className="eyebrow">Read-only demo</p>
        <h1>Editing is unavailable</h1>
        <Link className="button-secondary" to="/contacts">Back to contacts</Link>
      </section>
    )
  }

  if (invalidId) {
    return (
      <section className="content-card">
        <h1>Invalid contact</h1>
        <Link className="button-secondary" to="/contacts">Back to contacts</Link>
      </section>
    )
  }

  return (
    <section className="content-card">
      <p className="eyebrow">Contacts</p>
      <h1>{isEdit ? 'Edit contact' : 'Add contact'}</h1>
      {loading
        ? <p>Loading form...</p>
        : companies.length === 0
          ? <p>Create at least one company before adding a contact.</p>
          : (
            <ContactForm
              key={contact?.id ?? 'new'}
              contact={contact}
              companies={companies}
              submitting={submitting}
              error={error}
              onSubmit={handleSubmit}
              onCancel={() => navigate(isEdit ? `/contacts/${numericContactId}` : '/contacts')}
              readOnly={false}
            />
          )}
    </section>
  )
}
