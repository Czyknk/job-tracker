export type UserRole = 'ADMIN' | 'DEMO'

export interface LoginRequest {
  email: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export interface AuthUser {
  id: number
  role: UserRole
  email: string | null
}

export interface CurrentUserResponse {
  id: number
  email: string
  role: UserRole
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}
