import { apiFetch } from '../../shared/api/http'
import type { OfferSummaryResponse } from './summaryTypes'

export function listOfferSummaries(
  applicationId: number,
  accessToken: string,
): Promise<OfferSummaryResponse[]> {
  return apiFetch<OfferSummaryResponse[]>(
    `/applications/${applicationId}/summaries`,
    {},
    accessToken,
  )
}

export function generateOfferSummary(
  applicationId: number,
  accessToken: string,
): Promise<OfferSummaryResponse> {
  return apiFetch<OfferSummaryResponse>(
    `/applications/${applicationId}/summaries`,
    { method: 'POST' },
    accessToken,
  )
}
