export interface ContactResponse {
  id: number
  companyId: number
  companyName: string
  firstName: string
  lastName: string | null
  position: string | null
  email: string | null
  phone: string | null
  linkedinUrl: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface ContactRequest {
  companyId: number
  firstName: string
  lastName: string | null
  position: string | null
  email: string | null
  phone: string | null
  linkedinUrl: string | null
  notes: string | null
}
