import { apiFetch } from '../../shared/api/http'
import type { ContactRequest, ContactResponse } from './contactTypes'

export function listContacts(
  accessToken: string,
  companyId?: number,
): Promise<ContactResponse[]> {
  const suffix = companyId ? `?companyId=${companyId}` : ''
  return apiFetch<ContactResponse[]>(`/contacts${suffix}`, {}, accessToken)
}

export function getContact(
  contactId: number,
  accessToken: string,
): Promise<ContactResponse> {
  return apiFetch<ContactResponse>(`/contacts/${contactId}`, {}, accessToken)
}

export function createContact(
  request: ContactRequest,
  accessToken: string,
): Promise<ContactResponse> {
  return apiFetch<ContactResponse>('/contacts', {
    method: 'POST',
    body: JSON.stringify(request),
  }, accessToken)
}

export function updateContact(
  contactId: number,
  request: ContactRequest,
  accessToken: string,
): Promise<ContactResponse> {
  return apiFetch<ContactResponse>(`/contacts/${contactId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  }, accessToken)
}

export function deleteContact(
  contactId: number,
  accessToken: string,
): Promise<void> {
  return apiFetch<void>(`/contacts/${contactId}`, {
    method: 'DELETE',
  }, accessToken)
}
