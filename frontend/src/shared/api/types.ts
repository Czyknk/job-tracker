export interface ProblemDetail {
  type?: string
  title: string
  status: number
  detail?: string
  errors?: Record<string, string>
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
