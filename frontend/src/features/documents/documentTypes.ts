export type DocumentType = 'CV' | 'COVER_LETTER' | 'OTHER'

export interface DocumentResponse {
  id: number
  type: DocumentType
  name: string
  uploadDate: string
  archived: boolean
  hasFile: boolean
  contentType: string | null
  fileSize: number | null
  createdAt: string
  updatedAt: string
}

export interface UpdateDocumentRequest {
  name: string
  type: DocumentType
  archived: boolean
}
