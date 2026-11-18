import { apiFetch } from '../../shared/api/http'
import type { PageResponse } from '../../shared/api/types'
import type {
  ApplicationRequest,
  ApplicationResponse,
  ChangeStatusRequest,
  RecruitmentStatus,
  StatusHistoryResponse,
} from './applicationTypes'

export interface ApplicationFilters {
  status?: RecruitmentStatus
  companyId?: number
  page: number
  size: number
  sort: string
}

export function listApplications(
  accessToken: string,
  filters: ApplicationFilters,
): Promise<PageResponse<ApplicationResponse>> {
  const params = new URLSearchParams({
    page: String(filters.page),
    size: String(filters.size),
    sort: filters.sort,
  })
  if (filters.status) params.set('status', filters.status)
  if (filters.companyId) params.set('companyId', String(filters.companyId))

  return apiFetch<PageResponse<ApplicationResponse>>(
    `/applications?${params.toString()}`,
    {},
    accessToken,
  )
}

export function getApplication(
  applicationId: number,
  accessToken: string,
): Promise<ApplicationResponse> {
  return apiFetch<ApplicationResponse>(`/applications/${applicationId}`, {}, accessToken)
}

export function createApplication(
  request: ApplicationRequest,
  accessToken: string,
): Promise<ApplicationResponse> {
  return apiFetch<ApplicationResponse>('/applications', {
    method: 'POST',
    body: JSON.stringify(request),
  }, accessToken)
}

export function updateApplication(
  applicationId: number,
  request: ApplicationRequest,
  accessToken: string,
): Promise<ApplicationResponse> {
  return apiFetch<ApplicationResponse>(`/applications/${applicationId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  }, accessToken)
}

export function deleteApplication(
  applicationId: number,
  accessToken: string,
): Promise<void> {
  return apiFetch<void>(`/applications/${applicationId}`, {
    method: 'DELETE',
  }, accessToken)
}

export function getApplicationHistory(
  applicationId: number,
  accessToken: string,
): Promise<StatusHistoryResponse[]> {
  return apiFetch<StatusHistoryResponse[]>(
    `/applications/${applicationId}/history`,
    {},
    accessToken,
  )
}

export function changeApplicationStatus(
  applicationId: number,
  request: ChangeStatusRequest,
  accessToken: string,
): Promise<StatusHistoryResponse> {
  return apiFetch<StatusHistoryResponse>(
    `/applications/${applicationId}/status`,
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    accessToken,
  )
}
