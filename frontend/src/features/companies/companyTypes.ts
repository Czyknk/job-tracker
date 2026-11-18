export interface CompanyResponse {
  id: number
  name: string
  industry: string | null
  website: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface CompanyRequest {
  name: string
  industry: string | null
  website: string | null
  notes: string | null
}
