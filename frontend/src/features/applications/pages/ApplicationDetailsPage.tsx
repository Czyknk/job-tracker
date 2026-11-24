import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import { listContacts } from '../../contacts/contactApi'
import type { ContactResponse } from '../../contacts/contactTypes'
import {
  attachDocument,
  detachDocument,
  listApplicationDocuments,
} from '../../documents/applicationDocumentApi'
import type { DocumentResponse } from '../../documents/documentTypes'
import { listDocuments } from '../../documents/documentApi'
import {
  generateOfferSummary,
  listOfferSummaries,
} from '../../documents/summaryApi'
import type { OfferSummaryResponse } from '../../documents/summaryTypes'
import { downloadDocument } from '../../documents/documentApi'
import {
  changeApplicationStatus,
  deleteApplication,
  getApplication,
  getApplicationHistory,
  deleteHistoryEntry,
  updateHistoryEntry,
  updateApplicationNotes,
  archiveApplication,
  unarchiveApplication,
} from '../applicationApi'
import type {
  ApplicationResponse,
  RecruitmentStatus,
  StatusHistoryResponse,
} from '../applicationTypes'

const recruitmentStatuses: RecruitmentStatus[] = [
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

const terminalStatuses: RecruitmentStatus[] = [
  'ACCEPTED',
  'REJECTED',
  'WITHDRAWN',
]

export function ApplicationDetailsPage() {
  const { applicationId } = useParams()
  const numericApplicationId = Number(applicationId)
  const invalidId = !Number.isInteger(numericApplicationId)
  const { accessToken, user } = useAuth()
  const navigate = useNavigate()
  const [application, setApplication] = useState<ApplicationResponse | null>(null)
  const [loading, setLoading] = useState(!invalidId)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')
  const [history, setHistory] = useState<StatusHistoryResponse[]>([])
  const [contacts, setContacts] = useState<ContactResponse[]>([])
  const [newStatus, setNewStatus] = useState<RecruitmentStatus>('APPLIED')
  const [changedAt, setChangedAt] = useState('')
  const [statusNote, setStatusNote] = useState('')
  const [selectedContactIds, setSelectedContactIds] = useState<number[]>([])
  const [statusSubmitting, setStatusSubmitting] = useState(false)
  const [statusError, setStatusError] = useState('')
  const [applicationDocuments, setApplicationDocuments] = useState<DocumentResponse[]>([])
  const [libraryDocuments, setLibraryDocuments] = useState<DocumentResponse[]>([])
  const [selectedDocumentId, setSelectedDocumentId] = useState<number | ''>('')
  const [documentError, setDocumentError] = useState('')
  const [summaries, setSummaries] = useState<OfferSummaryResponse[]>([])
  const [summaryError, setSummaryError] = useState('')
  const [generatingSummary, setGeneratingSummary] = useState(false)
  const [notesDraft, setNotesDraft] = useState('')
  const [notesError, setNotesError] = useState('')
  const [historyError, setHistoryError] = useState('')
  const [editingHistoryId, setEditingHistoryId] = useState<number | null>(null)
  const [editingStatus, setEditingStatus] = useState<RecruitmentStatus>('FOUND')
  const [editingChangedAt, setEditingChangedAt] = useState('')
  const [editingNote, setEditingNote] = useState('')

  useEffect(() => {
    if (!accessToken || invalidId) return

    let active = true
    Promise.all([
      getApplication(numericApplicationId, accessToken),
      getApplicationHistory(numericApplicationId, accessToken),
      listApplicationDocuments(numericApplicationId, accessToken),
      listDocuments(accessToken),
      listOfferSummaries(numericApplicationId, accessToken).catch((requestError: unknown) => {
        if (requestError instanceof ApiError && requestError.status === 404) {
          return []
        }
        throw requestError
      }),
    ])
      .then(async ([applicationResult, historyResult, applicationDocumentsResult,
        libraryDocumentsResult, summariesResult]) => {
        if (!active) return
        setApplication(applicationResult)
        setNotesDraft(applicationResult.notes ?? '')
        setHistory(historyResult)
        setApplicationDocuments(applicationDocumentsResult)
        setLibraryDocuments(libraryDocumentsResult)
        setSummaries(summariesResult)
        setNewStatus(applicationResult.currentStatus)
        const contactResult = await listContacts(accessToken, applicationResult.companyId)
        if (active) setContacts(contactResult)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to load the application.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [accessToken, invalidId, numericApplicationId])

  async function handleDelete() {
    if (!accessToken || !application) return
    if (!window.confirm(`Delete "${application.position}"?`)) return

    setDeleting(true)
    setError('')

    try {
      await deleteApplication(application.id, accessToken)
      navigate('/applications')
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to delete the application.')
    } finally {
      setDeleting(false)
    }
  }

  async function handleStatusSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!accessToken || !application) return

    setStatusSubmitting(true)
    setStatusError('')

    try {
      const entry = await changeApplicationStatus(application.id, {
        status: newStatus,
        changedAt: changedAt ? new Date(changedAt).toISOString() : null,
        note: statusNote.trim() || null,
        contactIds: selectedContactIds,
      }, accessToken)
      void entry
      await reloadApplicationAndHistory()
      setStatusNote('')
      setChangedAt('')
      setSelectedContactIds([])
    } catch (requestError) {
      setStatusError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to change the status.')
    } finally {
      setStatusSubmitting(false)
    }
  }

  async function handleAttachDocument() {
    if (!accessToken || !application || selectedDocumentId === '') return
    setDocumentError('')

    try {
      await attachDocument(application.id, selectedDocumentId, accessToken)
      const attached = libraryDocuments.find((document) => document.id === selectedDocumentId)
      if (attached) {
        setApplicationDocuments((current) => [...current, attached])
      }
      setSelectedDocumentId('')
    } catch (requestError) {
      setDocumentError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to attach the document.')
    }
  }

  async function handleDetachDocument(documentId: number) {
    if (!accessToken || !application) return
    setDocumentError('')

    try {
      await detachDocument(application.id, documentId, accessToken)
      setApplicationDocuments((current) => current.filter((document) => document.id !== documentId))
    } catch (requestError) {
      setDocumentError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to detach the document.')
    }
  }

  async function handleGenerateSummary() {
    if (!accessToken || !application) return
    setGeneratingSummary(true)
    setSummaryError('')

    try {
      const summary = await generateOfferSummary(application.id, accessToken)
      setSummaries((current) => [summary, ...current])
    } catch (requestError) {
      setSummaryError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to generate the summary.')
    } finally {
      setGeneratingSummary(false)
    }
  }

  async function reloadApplicationAndHistory() {
    if (!accessToken || !application) return
    const [applicationResult, historyResult] = await Promise.all([
      getApplication(application.id, accessToken),
      getApplicationHistory(application.id, accessToken),
    ])
    setApplication(applicationResult)
    setNotesDraft(applicationResult.notes ?? '')
    setHistory(historyResult)
  }

  async function handleSaveNotes() {
    if (!accessToken || !application) return
    setNotesError('')
    try {
      const result = await updateApplicationNotes(
        application.id,
        notesDraft,
        accessToken,
      )
      setApplication(result)
      setNotesDraft(result.notes ?? '')
    } catch (requestError) {
      setNotesError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to update notes.')
    }
  }

  async function handleArchive() {
    if (!accessToken || !application) return
    setError('')
    try {
      const result = application.archived
        ? await unarchiveApplication(application.id, accessToken)
        : await archiveApplication(application.id, accessToken)
      setApplication(result)
      setNotesDraft(result.notes ?? '')
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to change archive state.')
    }
  }

  function startHistoryEdit(entry: StatusHistoryResponse) {
    setEditingHistoryId(entry.id)
    setEditingStatus(entry.status)
    setEditingChangedAt(entry.changedAt.slice(0, 16))
    setEditingNote(entry.note ?? '')
  }

  async function handleSaveHistoryEntry() {
    if (!accessToken || !application || editingHistoryId === null) return
    setHistoryError('')
    try {
      await updateHistoryEntry(application.id, editingHistoryId, {
        status: editingStatus,
        changedAt: new Date(editingChangedAt).toISOString(),
        note: editingNote.trim() || null,
      }, accessToken)
      await reloadApplicationAndHistory()
      setEditingHistoryId(null)
    } catch (requestError) {
      setHistoryError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to edit the history entry.')
    }
  }

  async function handleDeleteHistoryEntry(entryId: number) {
    if (!accessToken || !application) return
    if (!window.confirm('Undo this history entry?')) return
    setHistoryError('')
    try {
      await deleteHistoryEntry(application.id, entryId, accessToken)
      await reloadApplicationAndHistory()
    } catch (requestError) {
      setHistoryError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to delete the history entry.')
    }
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
      <Link className="back-link" to="/applications">Back to applications</Link>
      {loading && <p>Loading application...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {application && (
        <>
          <div className="page-header detail-header">
            <div>
              <p className="eyebrow">{application.companyName}</p>
              <h1>{application.position}</h1>
            </div>
            {user?.role === 'ADMIN' && (
              <Link
                className="button-primary"
                to={`/applications/${application.id}/edit`}
              >
                Edit
              </Link>
            )}
          </div>

          <span className={`status-badge status-${application.currentStatus.toLowerCase()}`}>
            {application.currentStatus}
          </span>
          {application.archived && (
            <span className="status-badge status-archived">ARCHIVED</span>
          )}

          <dl className="company-details application-details">
            <div><dt>Source</dt><dd>{application.source}</dd></div>
            <div><dt>Location</dt><dd>{application.location || '-'}</dd></div>
            <div><dt>Work mode</dt><dd>{application.workMode || '-'}</dd></div>
            <div><dt>Employment</dt><dd>{application.employmentType || '-'}</dd></div>
            <div><dt>Seniority</dt><dd>{application.seniorityLevel || '-'}</dd></div>
            <div>
              <dt>Offer URL</dt>
              <dd>
                {application.offerUrl
                  ? <a href={application.offerUrl} target="_blank" rel="noreferrer">{application.offerUrl}</a>
                  : '-'}
              </dd>
            </div>
            <div><dt>Date found</dt><dd>{application.dateFound}</dd></div>
            <div><dt>Date applied</dt><dd>{application.dateApplied || '-'}</dd></div>
            <div><dt>Deadline</dt><dd>{application.deadline || '-'}</dd></div>
            <div>
              <dt>Salary</dt>
              <dd>
                {application.salary
                  ? `${application.salary.min ?? '-'} - ${application.salary.max ?? '-'} ${application.salary.currency ?? ''} ${application.salary.period ?? ''} ${application.salary.basis ?? ''}`
                  : '-'}
              </dd>
            </div>
            <div><dt>Description</dt><dd>{application.rawDescription || '-'}</dd></div>
          </dl>

          <section className="pipeline-section" aria-labelledby="application-notes-title">
            <div className="page-header">
              <div>
                <p className="eyebrow">Notes</p>
                <h2 id="application-notes-title">Application notes</h2>
              </div>
              {user?.role === 'ADMIN' && (
                <button
                  type="button"
                  className="button-secondary"
                  onClick={() => void handleArchive()}
                >
                  {application.archived ? 'Unarchive' : 'Archive'}
                </button>
              )}
            </div>
            {notesError && <p className="form-error" role="alert">{notesError}</p>}
            {user?.role === 'ADMIN' ? (
              <div className="notes-editor">
                <textarea
                  value={notesDraft}
                  onChange={(event) => setNotesDraft(event.target.value)}
                  rows={5}
                  placeholder="Private notes about this application"
                />
                <button
                  type="button"
                  className="button-primary"
                  onClick={() => void handleSaveNotes()}
                >
                  Save notes
                </button>
              </div>
            ) : (
              <p>{application.notes || '-'}</p>
            )}
          </section>

          <section className="pipeline-section" aria-labelledby="status-history-title">
            <div className="page-header">
              <div>
                <p className="eyebrow">Pipeline</p>
                <h2 id="status-history-title">Status history</h2>
              </div>
            </div>

            {history.length === 0
              ? <p>No status history has been added yet.</p>
              : (
                <ol className="status-timeline">
                  {historyError && <p className="form-error" role="alert">{historyError}</p>}
                  {[...history].reverse().map((entry, reverseIndex) => (
                    <li key={entry.id}>
                      <div className="timeline-marker" />
                      <div>
                        <strong>{entry.status}</strong>
                        <time dateTime={entry.changedAt}>
                          {new Date(entry.changedAt).toLocaleString()}
                        </time>
                        {entry.note && <p>{entry.note}</p>}
                        {entry.contacts.length > 0 && (
                          <div className="history-contacts">
                            {entry.contacts.map((contact) => (
                              <span key={contact.id}>
                                {contact.firstName} {contact.lastName ?? ''}
                              </span>
                            ))}
                          </div>
                        )}
                        {user?.role === 'ADMIN' && (
                          <div className="history-actions">
                            {reverseIndex === 0 && (
                              <button
                                type="button"
                                className="button-secondary"
                                onClick={() => void handleDeleteHistoryEntry(entry.id)}
                              >
                                Undo last change
                              </button>
                            )}
                            <button
                              type="button"
                              className="button-secondary"
                              onClick={() => startHistoryEdit(entry)}
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              className="button-danger"
                              onClick={() => void handleDeleteHistoryEntry(entry.id)}
                            >
                              Delete
                            </button>
                          </div>
                        )}
                      </div>
                    </li>
                  ))}
                </ol>
              )}

            {editingHistoryId !== null && (
              <form
                className="status-form"
                onSubmit={(event) => {
                  event.preventDefault()
                  void handleSaveHistoryEntry()
                }}
              >
                <h3>Edit history entry</h3>
                {historyError && <p className="form-error" role="alert">{historyError}</p>}
                <div className="form-grid">
                  <label>
                    Status
                    <select
                      value={editingStatus}
                      onChange={(event) =>
                        setEditingStatus(event.target.value as RecruitmentStatus)}
                    >
                      {recruitmentStatuses.map((status) => (
                        <option key={status} value={status}>{status}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Changed at
                    <input
                      type="datetime-local"
                      value={editingChangedAt}
                      onChange={(event) => setEditingChangedAt(event.target.value)}
                      required
                    />
                  </label>
                </div>
                <label>
                  Note
                  <textarea
                    value={editingNote}
                    onChange={(event) => setEditingNote(event.target.value)}
                    rows={3}
                    maxLength={2000}
                  />
                </label>
                <div className="form-actions">
                  <button
                    type="button"
                    className="button-secondary"
                    onClick={() => setEditingHistoryId(null)}
                  >
                    Cancel
                  </button>
                  <button type="submit">Save entry</button>
                </div>
              </form>
            )}

            {terminalStatuses.includes(application.currentStatus) ? (
              <p className="terminal-status-message">
                This application has a final status and cannot be changed further.
              </p>
            ) : user?.role === 'ADMIN' ? (
              <form className="status-form" onSubmit={handleStatusSubmit}>
                <h3>Change status</h3>
                {statusError && <p className="form-error" role="alert">{statusError}</p>}

                <div className="form-grid">
                  <label>
                    Status
                    <select
                      value={newStatus}
                      onChange={(event) =>
                        setNewStatus(event.target.value as RecruitmentStatus)}
                    >
                      {recruitmentStatuses.map((status) => (
                        <option key={status} value={status}>{status}</option>
                      ))}
                    </select>
                  </label>

                  <label>
                    Changed at
                    <input
                      type="datetime-local"
                      value={changedAt}
                      onChange={(event) => setChangedAt(event.target.value)}
                    />
                  </label>
                </div>

                <label>
                  Note
                  <textarea
                    value={statusNote}
                    onChange={(event) => setStatusNote(event.target.value)}
                    rows={3}
                    maxLength={2000}
                  />
                </label>

                {contacts.length > 0 && (
                  <fieldset className="contact-selection">
                    <legend>Contacts involved</legend>
                    {contacts.map((contact) => (
                      <label key={contact.id} className="contact-checkbox">
                        <input
                          type="checkbox"
                          checked={selectedContactIds.includes(contact.id)}
                          onChange={(event) => {
                            setSelectedContactIds((current) => event.target.checked
                              ? [...current, contact.id]
                              : current.filter((id) => id !== contact.id))
                          }}
                        />
                        {contact.firstName} {contact.lastName ?? ''}
                        {contact.position ? ` — ${contact.position}` : ''}
                      </label>
                    ))}
                  </fieldset>
                )}

                <div className="form-actions">
                  <button type="submit" disabled={statusSubmitting}>
                    {statusSubmitting ? 'Saving...' : 'Change status'}
                  </button>
                </div>
              </form>
            ) : null}
          </section>

          <section className="pipeline-section" aria-labelledby="documents-title">
            <div className="page-header">
              <div>
                <p className="eyebrow">Documents</p>
                <h2 id="documents-title">Attached documents</h2>
              </div>
            </div>

            {documentError && <p className="form-error" role="alert">{documentError}</p>}

            {applicationDocuments.length === 0
              ? <p>No documents are attached to this application.</p>
              : (
                <div className="application-list">
                  {applicationDocuments.map((document) => (
                    <article className="document-row" key={document.id}>
                      <div>
                        <strong>{document.name}</strong>
                        <span>{document.type}</span>
                      </div>
                      <div className="document-actions">
                        {document.hasFile && (
                          <button
                            type="button"
                            className="button-secondary"
                            onClick={() => {
                              if (accessToken) {
                                void downloadDocument(document, accessToken)
                              }
                            }}
                          >
                            Download
                          </button>
                        )}
                        {user?.role === 'ADMIN' && (
                          <button
                            type="button"
                            className="button-secondary"
                            onClick={() => void handleDetachDocument(document.id)}
                          >
                            Detach
                          </button>
                        )}
                      </div>
                    </article>
                  ))}
                </div>
              )}

            {user?.role === 'ADMIN' && (
              <div className="attach-document">
                <label>
                  Document
                  <select
                    value={selectedDocumentId}
                    onChange={(event) => setSelectedDocumentId(
                      event.target.value ? Number(event.target.value) : '',
                    )}
                  >
                    <option value="">Select document</option>
                    {libraryDocuments
                      .filter((document) => !document.archived)
                      .filter((document) => !applicationDocuments.some(
                        (attached) => attached.id === document.id,
                      ))
                      .map((document) => (
                        <option key={document.id} value={document.id}>
                          {document.name} ({document.type})
                        </option>
                      ))}
                  </select>
                </label>
                <button
                  type="button"
                  className="button-primary"
                  disabled={selectedDocumentId === ''}
                  onClick={() => void handleAttachDocument()}
                >
                  Attach
                </button>
              </div>
            )}
          </section>

          <section className="pipeline-section" aria-labelledby="summaries-title">
            <div className="page-header">
              <div>
                <p className="eyebrow">AI</p>
                <h2 id="summaries-title">Offer summaries</h2>
              </div>
              {user?.role === 'ADMIN' && (
                <button
                  type="button"
                  className="button-primary"
                  onClick={() => void handleGenerateSummary()}
                  disabled={generatingSummary}
                >
                  {generatingSummary ? 'Generating...' : 'Generate summary'}
                </button>
              )}
            </div>

            {summaryError && <p className="form-error" role="alert">{summaryError}</p>}

            {summaries.length === 0
              ? <p>No AI summaries have been generated yet.</p>
              : (
                <div className="summary-list">
                  {[...summaries].map((summary) => (
                    <article className="summary-card" key={summary.id}>
                      <div className="summary-meta">
                        <strong>{summary.modelUsed}</strong>
                        <time dateTime={summary.generatedAt}>
                          {new Date(summary.generatedAt).toLocaleString()}
                        </time>
                      </div>
                      <p>{summary.summaryText}</p>
                    </article>
                  ))}
                </div>
              )}
          </section>

          {user?.role === 'ADMIN' && (
            <button
              type="button"
              className="button-danger"
              onClick={handleDelete}
              disabled={deleting}
            >
              {deleting ? 'Deleting...' : 'Delete application'}
            </button>
          )}
        </>
      )}
    </section>
  )
}
