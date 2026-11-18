import { useEffect, useState } from 'react'
import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../../auth/useAuth'
import {
  deleteDocument,
  downloadDocument,
  listDocuments,
  updateDocument,
  uploadDocument,
} from '../documentApi'
import type { DocumentResponse, DocumentType } from '../documentTypes'
import { DocumentUploadForm } from '../components/DocumentUploadForm'

export function DocumentsPage() {
  const { accessToken, user } = useAuth()
  const [documents, setDocuments] = useState<DocumentResponse[]>([])
  const [type, setType] = useState<DocumentType | ''>('')
  const [includeArchived, setIncludeArchived] = useState(false)
  const [showUpload, setShowUpload] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accessToken) return

    let active = true
    listDocuments(accessToken, type || undefined, includeArchived)
      .then((result) => {
        if (active) setDocuments(result)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setError(requestError instanceof ApiError
          ? requestError.problem.detail ?? requestError.problem.title
          : 'Unable to load documents.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [accessToken, includeArchived, type])

  async function handleUpload(
    file: globalThis.File,
    name: string,
    documentType: DocumentType,
    uploadDate: string,
  ) {
    if (!accessToken) return
    setSubmitting(true)
    setError('')

    try {
      await uploadDocument(file, name, documentType, uploadDate, accessToken)
      setShowUpload(false)
      setLoading(true)
      const result = await listDocuments(
        accessToken,
        type || undefined,
        includeArchived,
      )
      setDocuments(result)
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to upload the document.')
    } finally {
      setSubmitting(false)
      setLoading(false)
    }
  }

  async function handleArchive(document: DocumentResponse) {
    if (!accessToken) return
    setError('')

    try {
      const updated = await updateDocument(document.id, {
        name: document.name,
        type: document.type,
        archived: !document.archived,
      }, accessToken)
      setDocuments((current) => includeArchived
        ? current.map((item) => item.id === updated.id ? updated : item)
        : current.filter((item) => item.id !== updated.id),
      )
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to update the document.')
    }
  }

  async function handleDelete(document: DocumentResponse) {
    if (!accessToken) return
    if (!window.confirm(`Delete "${document.name}"?`)) return
    setError('')

    try {
      await deleteDocument(document.id, accessToken)
      setDocuments((current) => current.filter((item) => item.id !== document.id))
    } catch (requestError) {
      setError(requestError instanceof ApiError
        ? requestError.problem.detail ?? requestError.problem.title
        : 'Unable to delete the document.')
    }
  }

  return (
    <section className="content-card">
      <div className="page-header">
        <div>
          <p className="eyebrow">Documents</p>
          <h1>Document library</h1>
        </div>
        {user?.role === 'ADMIN' && (
          <button
            type="button"
            className="button-primary"
            onClick={() => setShowUpload((current) => !current)}
          >
            {showUpload ? 'Close upload' : 'Upload document'}
          </button>
        )}
      </div>

      {showUpload && user?.role === 'ADMIN' && (
        <DocumentUploadForm
          submitting={submitting}
          error={error}
          onSubmit={handleUpload}
          onCancel={() => setShowUpload(false)}
        />
      )}

      <div className="application-filters document-filters">
        <label>
          Type
          <select
            value={type}
            onChange={(event) => setType(event.target.value as DocumentType | '')}
          >
            <option value="">All types</option>
            <option value="CV">CV</option>
            <option value="COVER_LETTER">Cover letter</option>
            <option value="OTHER">Other</option>
          </select>
        </label>

        <label className="archive-filter">
          <input
            type="checkbox"
            checked={includeArchived}
            onChange={(event) => setIncludeArchived(event.target.checked)}
          />
          Include archived
        </label>
      </div>

      {loading && <p>Loading documents...</p>}
      {error && !showUpload && <p className="form-error" role="alert">{error}</p>}

      {!loading && !error && documents.length === 0 && (
        <p>No documents found.</p>
      )}

      {documents.length > 0 && (
        <div className="application-list">
          {documents.map((document) => (
            <article className="document-row" key={document.id}>
              <div>
                <strong>{document.name}</strong>
                <span>{document.type}{document.archived ? ' · Archived' : ''}</span>
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
                  <>
                    <button
                      type="button"
                      className="button-secondary"
                      onClick={() => void handleArchive(document)}
                    >
                      {document.archived ? 'Restore' : 'Archive'}
                    </button>
                    <button
                      type="button"
                      className="button-danger"
                      onClick={() => void handleDelete(document)}
                    >
                      Delete
                    </button>
                  </>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
