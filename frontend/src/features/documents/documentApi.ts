import { apiFetch } from '../../shared/api/http'
import type {
  DocumentResponse,
  DocumentType,
  UpdateDocumentRequest,
} from './documentTypes'

export function listDocuments(
  accessToken: string,
  type?: DocumentType,
  includeArchived = false,
): Promise<DocumentResponse[]> {
  const params = new URLSearchParams({
    includeArchived: String(includeArchived),
  })
  if (type) params.set('type', type)

  return apiFetch<DocumentResponse[]>(`/documents?${params.toString()}`, {}, accessToken)
}

export function getDocument(
  documentId: number,
  accessToken: string,
): Promise<DocumentResponse> {
  return apiFetch<DocumentResponse>(`/documents/${documentId}`, {}, accessToken)
}

export function uploadDocument(
  file: File,
  name: string,
  type: DocumentType,
  uploadDate: string,
  accessToken: string,
): Promise<DocumentResponse> {
  const formData = new FormData()
  formData.set('file', file)
  if (name) formData.set('name', name)
  formData.set('type', type)
  if (uploadDate) formData.set('uploadDate', uploadDate)

  return apiFetch<DocumentResponse>('/documents/upload', {
    method: 'POST',
    body: formData,
  }, accessToken)
}

export function updateDocument(
  documentId: number,
  request: UpdateDocumentRequest,
  accessToken: string,
): Promise<DocumentResponse> {
  return apiFetch<DocumentResponse>(`/documents/${documentId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  }, accessToken)
}

export function deleteDocument(
  documentId: number,
  accessToken: string,
): Promise<void> {
  return apiFetch<void>(`/documents/${documentId}`, {
    method: 'DELETE',
  }, accessToken)
}

export function getDocumentDownloadUrl(documentId: number): string {
  return `/api/documents/${documentId}/file`
}

export async function downloadDocument(
  document: DocumentResponse,
  accessToken: string,
): Promise<void> {
  const response = await fetch(`/api/documents/${document.id}/file`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!response.ok) {
    throw new Error('Unable to download the document')
  }

  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const link = window.document.createElement('a')
  link.href = objectUrl
  link.download = document.name
  window.document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(objectUrl)
}
