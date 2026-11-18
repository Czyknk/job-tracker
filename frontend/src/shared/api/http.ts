import type { ProblemDetail } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title)
    this.name = 'ApiError'
    this.status = problem.status
    this.problem = problem
  }
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
  accessToken?: string,
): Promise<T> {
  const headers = new Headers(init.headers)

  if (
    init.body !== undefined
    && !(init.body instanceof FormData)
    && !headers.has('Content-Type')
  ) {
    headers.set('Content-Type', 'application/json')
  }
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const response = await fetch(`/api${path}`, {
    ...init,
    credentials: 'include',
    headers,
  })

  if (!response.ok) {
    let problem: ProblemDetail

    try {
      problem = await response.json() as ProblemDetail
    } catch {
      problem = {
        title: 'Request failed',
        status: response.status,
        detail: response.statusText,
      }
    }

    throw new ApiError(problem)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return await response.json() as T
}
