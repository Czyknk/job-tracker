import { useState, type FormEvent } from 'react'
import type { CompanyResponse } from '../../companies/companyTypes'
import type { ContactRequest, ContactResponse } from '../contactTypes'

interface ContactFormProps {
  contact?: ContactResponse
  companies: CompanyResponse[]
  submitting: boolean
  error: string
  onSubmit: (request: ContactRequest) => Promise<void>
  onCancel: () => void
  readOnly: boolean
}

export function ContactForm({
  contact,
  companies,
  submitting,
  error,
  onSubmit,
  onCancel,
  readOnly,
}: ContactFormProps) {
  const [companyId, setCompanyId] = useState(contact?.companyId ?? companies[0]?.id ?? 0)
  const [firstName, setFirstName] = useState(contact?.firstName ?? '')
  const [lastName, setLastName] = useState(contact?.lastName ?? '')
  const [position, setPosition] = useState(contact?.position ?? '')
  const [email, setEmail] = useState(contact?.email ?? '')
  const [phone, setPhone] = useState(contact?.phone ?? '')
  const [linkedinUrl, setLinkedinUrl] = useState(contact?.linkedinUrl ?? '')
  const [notes, setNotes] = useState(contact?.notes ?? '')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await onSubmit({
      companyId,
      firstName: firstName.trim(),
      lastName: lastName.trim() || null,
      position: position.trim() || null,
      email: email.trim() || null,
      phone: phone.trim() || null,
      linkedinUrl: linkedinUrl.trim() || null,
      notes: notes.trim() || null,
    })
  }

  return (
    <form className="application-form" onSubmit={handleSubmit}>
      {error && <p className="form-error" role="alert">{error}</p>}

      <div className="form-grid">
        <label>
          Company
          <select
            value={companyId}
            onChange={(event) => setCompanyId(Number(event.target.value))}
            required
            disabled={readOnly}
          >
            {companies.map((company) => (
              <option key={company.id} value={company.id}>{company.name}</option>
            ))}
          </select>
        </label>

        <label>
          First name
          <input
            value={firstName}
            onChange={(event) => setFirstName(event.target.value)}
            required
            maxLength={100}
            disabled={readOnly}
          />
        </label>

        <label>
          Last name
          <input
            value={lastName}
            onChange={(event) => setLastName(event.target.value)}
            maxLength={100}
            disabled={readOnly}
          />
        </label>

        <label>
          Position
          <input
            value={position}
            onChange={(event) => setPosition(event.target.value)}
            maxLength={255}
            disabled={readOnly}
          />
        </label>

        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            maxLength={255}
            disabled={readOnly}
          />
        </label>

        <label>
          Phone
          <input
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            maxLength={50}
            disabled={readOnly}
          />
        </label>

        <label>
          LinkedIn URL
          <input
            type="url"
            value={linkedinUrl}
            onChange={(event) => setLinkedinUrl(event.target.value)}
            maxLength={500}
            disabled={readOnly}
          />
        </label>
      </div>

      <label>
        Notes
        <textarea
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
          rows={5}
          disabled={readOnly}
        />
      </label>

      <div className="form-actions">
        <button type="button" className="button-secondary" onClick={onCancel}>
          Back
        </button>
        {!readOnly && (
          <button type="submit" disabled={submitting}>
            {submitting ? 'Saving...' : 'Save contact'}
          </button>
        )}
      </div>
    </form>
  )
}
