import { apiFetch } from '../../shared/api/http'
import type {
  ChangePasswordRequest,
  CurrentUserResponse,
  LoginRequest,
  TokenResponse,
} from './authTypes'

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

export function getCurrentUser(accessToken: string): Promise<CurrentUserResponse> {
  return apiFetch<CurrentUserResponse>('/auth/me', {}, accessToken)
}

export function changePassword(
  request: ChangePasswordRequest,
  accessToken: string,
): Promise<void> {
  return apiFetch<void>('/auth/change-password', {
    method: 'POST',
    body: JSON.stringify(request),
  }, accessToken)
}
