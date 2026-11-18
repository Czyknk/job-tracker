import { apiFetch } from '../../shared/api/http'
import type { LoginRequest, TokenResponse } from './authTypes'

let refreshRequest: Promise<TokenResponse> | null = null

export function login(request: LoginRequest): Promise<TokenResponse> {
  return apiFetch<TokenResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function refreshAccessToken(): Promise<TokenResponse> {
  if (!refreshRequest) {
    refreshRequest = apiFetch<TokenResponse>('/auth/refresh', {
      method: 'POST',
    }).finally(() => {
      refreshRequest = null
    })
  }
  return refreshRequest
}

export function logout(): Promise<void> {
  return apiFetch<void>('/auth/logout', {
    method: 'POST',
  })
}
