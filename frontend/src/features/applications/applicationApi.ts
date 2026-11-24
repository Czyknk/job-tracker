import { apiFetch } from '../../shared/api/http'
import type { PageResponse } from '../../shared/api/types'
import type {
  ApplicationRequest,
  ApplicationResponse,
  ApplicationListItem,
  ApplicationArchivedFilter,
  ChangeStatusRequest,
  EmploymentType,
  JobSource,
  RecruitmentStatus,
  StatusHistoryResponse,
  SeniorityLevel,
  WorkMode,
} from './applicationTypes'

export interface ApplicationFilters {
  status?: RecruitmentStatus
  companyId?: number
  page: number
  size: number
  sort: string
  q?: string
  source?: JobSource
  workMode?: WorkMode
  employmentType?: EmploymentType
  seniorityLevel?: SeniorityLevel
  foundFrom?: string
  foundTo?: string
  deadlineBefore?: string
  archived?: ApplicationArchivedFilter
}

export function listApplications(
  accessToken: string,
  filters: ApplicationFilters,
): Promise<PageResponse<ApplicationListItem>> {
  const params = new URLSearchParams({
    page: String(filters.page),
    size: String(filters.size),
    sort: filters.sort,
  })
  if (filters.status) params.set('status', filters.status)
  if (filters.companyId) params.set('companyId', String(filters.companyId))
  if (filters.q) params.set('q', filters.q)
  if (filters.source) params.set('source', filters.source)
  if (filters.workMode) params.set('workMode', filters.workMode)
  if (filters.employmentType) params.set('employmentType', filters.employmentType)
  if (filters.seniorityLevel) params.set('seniorityLevel', filters.seniorityLevel)
  if (filters.foundFrom) params.set('foundFrom', filters.foundFrom)
  if (filters.foundTo) params.set('foundTo', filters.foundTo)
  if (filters.deadlineBefore) params.set('deadlineBefore', filters.deadlineBefore)
  if (filters.archived) params.set('archived', filters.archived)

  return apiFetch<PageResponse<ApplicationListItem>>(
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

export function updateApplicationNotes(
  applicationId: number,
  notes: string,
  accessToken: string,
): Promise<ApplicationResponse> {
  return apiFetch<ApplicationResponse>(`/applications/${applicationId}/notes`, {
    method: 'PUT',
    body: JSON.stringify({ notes }),
  }, accessToken)
}

export function archiveApplication(
  applicationId: number,
  accessToken: string,
): Promise<ApplicationResponse> {
  return apiFetch<ApplicationResponse>(
    `/applications/${applicationId}/archive`,
    { method: 'POST' },
    accessToken,
  )
}

export function unarchiveApplication(
  applicationId: number,
  accessToken: string,
): Promise<ApplicationResponse> {
  return apiFetch<ApplicationResponse>(
    `/applications/${applicationId}/unarchive`,
    { method: 'POST' },
    accessToken,
  )
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

export function updateHistoryEntry(
  applicationId: number,
  entryId: number,
  request: {
    status: RecruitmentStatus
    changedAt: string
    note: string | null
  },
  accessToken: string,
): Promise<StatusHistoryResponse> {
  return apiFetch<StatusHistoryResponse>(
    `/applications/${applicationId}/history/${entryId}`,
    {
      method: 'PUT',
      body: JSON.stringify(request),
    },
    accessToken,
  )
}

export function deleteHistoryEntry(
  applicationId: number,
  entryId: number,
  accessToken: string,
): Promise<void> {
  return apiFetch<void>(
    `/applications/${applicationId}/history/${entryId}`,
    { method: 'DELETE' },
    accessToken,
  )
}
