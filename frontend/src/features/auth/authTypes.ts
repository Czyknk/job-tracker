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
  role: UserRole
  email: string | null
}
