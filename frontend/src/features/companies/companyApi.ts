import { apiFetch } from '../../shared/api/http'
import type { CompanyRequest, CompanyResponse } from './companyTypes'

export function listCompanies(accessToken: string): Promise<CompanyResponse[]> {
  return apiFetch<CompanyResponse[]>('/companies', {}, accessToken)
}

export function getCompany(
  companyId: number,
  accessToken: string,
): Promise<CompanyResponse> {
  return apiFetch<CompanyResponse>(`/companies/${companyId}`, {}, accessToken)
}

export function createCompany(
  request: CompanyRequest,
  accessToken: string,
): Promise<CompanyResponse> {
  return apiFetch<CompanyResponse>('/companies', {
    method: 'POST',
    body: JSON.stringify(request),
  }, accessToken)
}

export function updateCompany(
  companyId: number,
  request: CompanyRequest,
  accessToken: string,
): Promise<CompanyResponse> {
  return apiFetch<CompanyResponse>(`/companies/${companyId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  }, accessToken)
}

export function deleteCompany(
  companyId: number,
  accessToken: string,
): Promise<void> {
  return apiFetch<void>(`/companies/${companyId}`, {
    method: 'DELETE',
  }, accessToken)
}
