import {
  createContext,
} from 'react'

import type { AuthUser, ChangePasswordRequest, LoginRequest } from './authTypes'

export interface AuthContextValue {
  accessToken: string | null
  user: AuthUser | null
  isLoading: boolean
  passwordChanged: boolean
  login: (request: LoginRequest) => Promise<void>
  logout: () => Promise<void>
  changePassword: (request: ChangePasswordRequest) => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
