import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError } from '../../shared/api/http'
import { AuthContext, type AuthContextValue } from './authContext'
import { login, logout, refreshAccessToken } from './authApi'
import type { AuthUser, TokenResponse, UserRole } from './authTypes'

interface JwtPayload {
  role?: UserRole
}

function readJwtPayload(accessToken: string): JwtPayload | null {
  const parts = accessToken.split('.')
  if (parts.length !== 3) return null

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(base64)) as JwtPayload
  } catch {
    return null
  }
}

function isValidUserRole(role: unknown): role is UserRole {
  return role === 'ADMIN' || role === 'DEMO'
}

function createUser(tokenResponse: TokenResponse, email: string | null): AuthUser | null {
  const payload = readJwtPayload(tokenResponse.accessToken)
  if (!payload || !isValidUserRole(payload.role)) return null
  return { role: payload.role, email }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let active = true

    refreshAccessToken()
      .then((tokenResponse) => {
        if (!active) return
        setAccessToken(tokenResponse.accessToken)
        setUser(createUser(tokenResponse, null))
      })
      .catch((error: unknown) => {
        if (!active) return
        if (!(error instanceof ApiError) || error.status !== 401) {
          console.error('Session refresh failed', error)
        }
        setAccessToken(null)
        setUser(null)
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    accessToken,
    user,
    isLoading,
    login: async (request) => {
      const tokenResponse = await login(request)
      setAccessToken(tokenResponse.accessToken)
      setUser(createUser(tokenResponse, request.email))
    },
    logout: async () => {
      try {
        await logout()
      } finally {
        setAccessToken(null)
        setUser(null)
      }
    },
  }), [accessToken, user, isLoading])

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}
