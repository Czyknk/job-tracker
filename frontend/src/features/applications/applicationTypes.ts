export type RecruitmentStatus =
  | 'FOUND'
  | 'APPLIED'
  | 'HR_SCREEN'
  | 'TECH_TASK'
  | 'TECH_INTERVIEW'
  | 'FINAL_INTERVIEW'
  | 'OFFER_RECEIVED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'GHOSTED'

export type JobSource =
  | 'PRACUJ_PL'
  | 'LINKEDIN'
  | 'THE_PROTOCOL'
  | 'JUST_JOIN_IT'
  | 'OTHER'

export type WorkMode = 'REMOTE' | 'HYBRID' | 'ONSITE'
export type EmploymentType = 'UOP' | 'B2B' | 'ZLECENIE' | 'OTHER'
export type SeniorityLevel = 'INTERN' | 'JUNIOR' | 'MID' | 'SENIOR'
export type SalaryPeriod = 'MONTH' | 'HOUR'
export type SalaryBasis = 'GROSS' | 'NET'

export interface SalaryRangeDto {
  min: string | null
  max: string | null
  currency: string | null
  period: SalaryPeriod | null
  basis: SalaryBasis | null
}

export interface ApplicationResponse {
  id: number
  companyId: number
  companyName: string
  position: string
  offerUrl: string | null
  source: JobSource
  location: string | null
  workMode: WorkMode | null
  employmentType: EmploymentType | null
  seniorityLevel: SeniorityLevel | null
  salary: SalaryRangeDto | null
  rawDescription: string | null
  deadline: string | null
  dateFound: string
  dateApplied: string | null
  currentStatus: RecruitmentStatus
  createdAt: string
  updatedAt: string
}

export interface ApplicationRequest {
  companyId: number
  position: string
  offerUrl: string | null
  source: JobSource
  location: string | null
  workMode: WorkMode | null
  employmentType: EmploymentType | null
  seniorityLevel: SeniorityLevel | null
  salary: SalaryRangeDto | null
  rawDescription: string | null
  deadline: string | null
  dateFound: string
  dateApplied: string | null
}
