import { useState, type FormEvent } from 'react'
import type { DocumentType } from '../documentTypes'

interface DocumentUploadFormProps {
  submitting: boolean
  error: string
  onSubmit: (
    file: File,
    name: string,
    type: DocumentType,
    uploadDate: string,
  ) => Promise<void>
  onCancel: () => void
}

const today = new Date().toISOString().slice(0, 10)

export function DocumentUploadForm({
  submitting,
  error,
  onSubmit,
  onCancel,
}: DocumentUploadFormProps) {
  const [file, setFile] = useState<File | null>(null)
  const [name, setName] = useState('')
  const [type, setType] = useState<DocumentType>('CV')
  const [uploadDate, setUploadDate] = useState(today)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) return
    await onSubmit(file, name.trim(), type, uploadDate)
  }

  return (
    <form className="application-form" onSubmit={handleSubmit}>
      {error && <p className="form-error" role="alert">{error}</p>}

      <label>
        File
        <input
          type="file"
          required
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
        />
      </label>

      <div className="form-grid">
        <label>
          Name
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={255}
            placeholder="Optional"
          />
        </label>

        <label>
          Type
          <select
            value={type}
            onChange={(event) => setType(event.target.value as DocumentType)}
          >
            <option value="CV">CV</option>
            <option value="COVER_LETTER">Cover letter</option>
            <option value="OTHER">Other</option>
          </select>
        </label>

        <label>
          Upload date
          <input
            type="date"
            value={uploadDate}
            onChange={(event) => setUploadDate(event.target.value)}
          />
        </label>
      </div>

      <div className="form-actions">
        <button type="button" className="button-secondary" onClick={onCancel}>
          Cancel
        </button>
        <button type="submit" disabled={submitting}>
          {submitting ? 'Uploading...' : 'Upload document'}
        </button>
      </div>
    </form>
  )
}
