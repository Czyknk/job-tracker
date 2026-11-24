import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError } from '../../shared/api/http'
import { AuthContext, type AuthContextValue } from './authContext'
import { changePassword, getCurrentUser, login, logout, refreshAccessToken } from './authApi'
import type { AuthUser } from './authTypes'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [passwordChanged, setPasswordChanged] = useState(false)

  useEffect(() => {
    let active = true

    refreshAccessToken()
      .then(async (tokenResponse) => {
        if (!active) return
        setAccessToken(tokenResponse.accessToken)
        const currentUser = await getCurrentUser(tokenResponse.accessToken)
        if (active) setUser({
          id: currentUser.id,
          email: currentUser.email,
          role: currentUser.role,
        })
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
    passwordChanged,
    login: async (request) => {
      setPasswordChanged(false)
      const tokenResponse = await login(request)
      setAccessToken(tokenResponse.accessToken)
      const currentUser = await getCurrentUser(tokenResponse.accessToken)
      setUser({
        id: currentUser.id,
        email: currentUser.email,
        role: currentUser.role,
      })
    },
    logout: async () => {
      try {
        await logout()
      } finally {
        setAccessToken(null)
        setUser(null)
      }
    },
    changePassword: async (request) => {
      if (!accessToken) throw new Error('No active session')
      await changePassword(request, accessToken)
      setPasswordChanged(true)
      setAccessToken(null)
      setUser(null)
    },
  }), [accessToken, user, isLoading, passwordChanged])

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}
