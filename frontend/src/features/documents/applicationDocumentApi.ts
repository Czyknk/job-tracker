import { apiFetch } from '../../shared/api/http'
import type { DocumentResponse } from './documentTypes'

export function listApplicationDocuments(
  applicationId: number,
  accessToken: string,
): Promise<DocumentResponse[]> {
  return apiFetch<DocumentResponse[]>(
    `/applications/${applicationId}/documents`,
    {},
    accessToken,
  )
}

export function attachDocument(
  applicationId: number,
  documentId: number,
  accessToken: string,
): Promise<void> {
  return apiFetch<void>(
    `/applications/${applicationId}/documents/${documentId}`,
    { method: 'PUT' },
    accessToken,
  )
}

export function detachDocument(
  applicationId: number,
  documentId: number,
  accessToken: string,
): Promise<void> {
  return apiFetch<void>(
    `/applications/${applicationId}/documents/${documentId}`,
    { method: 'DELETE' },
    accessToken,
  )
}
