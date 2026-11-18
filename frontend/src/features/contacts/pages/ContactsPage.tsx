import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { listCompanies } from '../../companies/companyApi'
import type { CompanyResponse } from '../../companies/companyTypes'
import { listContacts } from '../contactApi'
import type { ContactResponse } from '../contactTypes'

export function ContactsPage() {
  const { accessToken, user } = useAuth()
  const [contacts, setContacts] = useState<ContactResponse[]>([])
  const [companies, setCompanies] = useState<CompanyResponse[]>([])
  const [companyId, setCompanyId] = useState<number | ''>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accessToken) return

    let active = true
    Promise.all([
      listCompanies(accessToken),
      listContacts(accessToken, companyId || undefined),
    ])
      .then(([companyResult, contactResult]) => {
        if (!active) return
        setCompanies(companyResult)
        setContacts(contactResult)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to load contacts.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [accessToken, companyId])

  return (
    <section className="content-card">
      <div className="page-header">
        <div>
          <p className="eyebrow">Contacts</p>
          <h1>Recruitment contacts</h1>
        </div>
        {user?.role === 'ADMIN' && (
          <Link className="button-primary" to="/contacts/new">Add contact</Link>
        )}
      </div>

      <div className="application-filters">
        <label>
          Company
          <select
            value={companyId}
            onChange={(event) => setCompanyId(event.target.value ? Number(event.target.value) : '')}
          >
            <option value="">All companies</option>
            {companies.map((company) => (
              <option key={company.id} value={company.id}>{company.name}</option>
            ))}
          </select>
        </label>
      </div>

      {loading && <p>Loading contacts...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {!loading && !error && contacts.length === 0 && <p>No contacts found.</p>}

      {contacts.length > 0 && (
        <div className="application-list">
          {contacts.map((contact) => (
            <Link key={contact.id} className="application-row" to={`/contacts/${contact.id}`}>
              <div>
                <strong>{contact.firstName} {contact.lastName ?? ''}</strong>
                <span>{contact.position || contact.companyName}</span>
              </div>
              <div className="application-row-meta">
                <span>{contact.companyName}</span>
                {contact.email && <span>{contact.email}</span>}
              </div>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}
