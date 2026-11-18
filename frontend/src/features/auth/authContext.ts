import {
  createContext,
} from 'react'

import type { AuthUser, LoginRequest } from './authTypes'

export interface AuthContextValue {
  accessToken: string | null
  user: AuthUser | null
  isLoading: boolean
  login: (request: LoginRequest) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
