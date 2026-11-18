import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { deleteContact, getContact } from '../contactApi'
import type { ContactResponse } from '../contactTypes'

export function ContactDetailsPage() {
  const { contactId } = useParams()
  const numericContactId = Number(contactId)
  const invalidId = !Number.isInteger(numericContactId)
  const { accessToken, user } = useAuth()
  const navigate = useNavigate()
  const [contact, setContact] = useState<ContactResponse | null>(null)
  const [loading, setLoading] = useState(!invalidId)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accessToken || invalidId) return
    let active = true
    getContact(numericContactId, accessToken)
      .then((result) => {
        if (active) setContact(result)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to load the contact.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [accessToken, invalidId, numericContactId])

  async function handleDelete() {
    if (!accessToken || !contact) return
    if (!window.confirm(`Delete "${contact.firstName} ${contact.lastName ?? ''}"?`)) return
    setDeleting(true)
    setError('')
    try {
      await deleteContact(contact.id, accessToken)
      navigate('/contacts')
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to delete the contact.')
    } finally {
      setDeleting(false)
    }
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
      <Link className="back-link" to="/contacts">Back to contacts</Link>
      {loading && <p>Loading contact...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      {contact && (
        <>
          <div className="page-header detail-header">
            <div>
              <p className="eyebrow">{contact.companyName}</p>
              <h1>{contact.firstName} {contact.lastName ?? ''}</h1>
            </div>
            {user?.role === 'ADMIN' && (
              <Link className="button-primary" to={`/contacts/${contact.id}/edit`}>Edit</Link>
            )}
          </div>

          <dl className="company-details application-details">
            <div><dt>Position</dt><dd>{contact.position || '-'}</dd></div>
            <div><dt>Email</dt><dd>{contact.email || '-'}</dd></div>
            <div><dt>Phone</dt><dd>{contact.phone || '-'}</dd></div>
            <div>
              <dt>LinkedIn</dt>
              <dd>
                {contact.linkedinUrl
                  ? <a href={contact.linkedinUrl} target="_blank" rel="noreferrer">{contact.linkedinUrl}</a>
                  : '-'}
              </dd>
            </div>
            <div><dt>Notes</dt><dd>{contact.notes || '-'}</dd></div>
          </dl>

          {user?.role === 'ADMIN' && (
            <button
              type="button"
              className="button-danger"
              onClick={handleDelete}
              disabled={deleting}
            >
              {deleting ? 'Deleting...' : 'Delete contact'}
            </button>
          )}
        </>
      )}
    </section>
  )
}
