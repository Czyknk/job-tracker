import { useState, type FormEvent } from 'react'
import type { CompanyRequest, CompanyResponse } from '../companyTypes'

interface CompanyFormProps {
  company?: CompanyResponse
  submitting: boolean
  error: string
  onSubmit: (request: CompanyRequest) => Promise<void>
  onCancel: () => void
  readOnly: boolean
}

export function CompanyForm({
  company,
  submitting,
  error,
  onSubmit,
  onCancel,
  readOnly,
}: CompanyFormProps) {
  const [name, setName] = useState(company?.name ?? '')
  const [industry, setIndustry] = useState(company?.industry ?? '')
  const [website, setWebsite] = useState(company?.website ?? '')
  const [notes, setNotes] = useState(company?.notes ?? '')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    await onSubmit({
      name: name.trim(),
      industry: industry.trim() || null,
      website: website.trim() || null,
      notes: notes.trim() || null,
    })
  }

  return (
    <form className="company-form" onSubmit={handleSubmit}>
      {error && <p className="form-error" role="alert">{error}</p>}

      <label>
        Name
        <input
          value={name}
          onChange={(event) => setName(event.target.value)}
          required
          maxLength={255}
          disabled={readOnly}
        />
      </label>

      <label>
        Industry
        <input
          value={industry}
          onChange={(event) => setIndustry(event.target.value)}
          maxLength={255}
          disabled={readOnly}
        />
      </label>

      <label>
        Website
        <input
          type="url"
          value={website}
          onChange={(event) => setWebsite(event.target.value)}
          maxLength={500}
          placeholder="https://example.com"
          disabled={readOnly}
        />
      </label>

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
            {submitting ? 'Saving...' : 'Save company'}
          </button>
        )}
      </div>
    </form>
  )
}
