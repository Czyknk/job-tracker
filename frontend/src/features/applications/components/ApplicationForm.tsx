import { useState, type FormEvent } from 'react'
import type { CompanyResponse } from '../../companies/companyTypes'
import type {
  ApplicationRequest,
  ApplicationResponse,
  EmploymentType,
  JobSource,
  SalaryBasis,
  SalaryPeriod,
  SeniorityLevel,
  WorkMode,
} from '../applicationTypes'

interface ApplicationFormProps {
  application?: ApplicationResponse
  companies: CompanyResponse[]
  submitting: boolean
  error: string
  onSubmit: (request: ApplicationRequest) => Promise<void>
  onCancel: () => void
  readOnly: boolean
}

const jobSources: JobSource[] = [
  'PRACUJ_PL',
  'LINKEDIN',
  'THE_PROTOCOL',
  'JUST_JOIN_IT',
  'OTHER',
]

const workModes: WorkMode[] = ['REMOTE', 'HYBRID', 'ONSITE']
const employmentTypes: EmploymentType[] = ['UOP', 'B2B', 'ZLECENIE', 'OTHER']
const seniorityLevels: SeniorityLevel[] = ['INTERN', 'JUNIOR', 'MID', 'SENIOR']
const salaryPeriods: SalaryPeriod[] = ['MONTH', 'HOUR']
const salaryBases: SalaryBasis[] = ['GROSS', 'NET']
const today = new Date().toISOString().slice(0, 10)

export function ApplicationForm({
  application,
  companies,
  submitting,
  error,
  onSubmit,
  onCancel,
  readOnly,
}: ApplicationFormProps) {
  const [companyId, setCompanyId] = useState(application?.companyId ?? companies[0]?.id ?? 0)
  const [position, setPosition] = useState(application?.position ?? '')
  const [offerUrl, setOfferUrl] = useState(application?.offerUrl ?? '')
  const [source, setSource] = useState<JobSource>(application?.source ?? 'OTHER')
  const [location, setLocation] = useState(application?.location ?? '')
  const [workMode, setWorkMode] = useState<WorkMode | ''>(application?.workMode ?? '')
  const [employmentType, setEmploymentType] = useState<EmploymentType | ''>(
    application?.employmentType ?? '',
  )
  const [seniorityLevel, setSeniorityLevel] = useState<SeniorityLevel | ''>(
    application?.seniorityLevel ?? '',
  )
  const [salaryMin, setSalaryMin] = useState(application?.salary?.min ?? '')
  const [salaryMax, setSalaryMax] = useState(application?.salary?.max ?? '')
  const [currency, setCurrency] = useState(application?.salary?.currency ?? '')
  const [salaryPeriod, setSalaryPeriod] = useState<SalaryPeriod | ''>(
    application?.salary?.period ?? '',
  )
  const [salaryBasis, setSalaryBasis] = useState<SalaryBasis | ''>(
    application?.salary?.basis ?? '',
  )
  const [rawDescription, setRawDescription] = useState(application?.rawDescription ?? '')
  const [deadline, setDeadline] = useState(application?.deadline ?? '')
  const [dateFound, setDateFound] = useState(
    application?.dateFound ?? today,
  )
  const [dateApplied, setDateApplied] = useState(application?.dateApplied ?? '')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const hasSalary = Boolean(
      salaryMin || salaryMax || currency || salaryPeriod || salaryBasis,
    )

    await onSubmit({
      companyId,
      position: position.trim(),
      offerUrl: offerUrl.trim() || null,
      source,
      location: location.trim() || null,
      workMode: workMode || null,
      employmentType: employmentType || null,
      seniorityLevel: seniorityLevel || null,
      salary: hasSalary
        ? {
            min: salaryMin || null,
            max: salaryMax || null,
            currency: currency.trim().toUpperCase() || null,
            period: salaryPeriod || null,
            basis: salaryBasis || null,
          }
        : null,
      rawDescription: rawDescription.trim() || null,
      deadline: deadline || null,
      dateFound,
      dateApplied: dateApplied || null,
    })
  }

  return (
    <form className="application-form" onSubmit={handleSubmit}>
      {error && <p className="form-error" role="alert">{error}</p>}

      <div className="form-grid">
        <label>
          Company
          <select
            value={companyId}
            onChange={(event) => setCompanyId(Number(event.target.value))}
            required
            disabled={readOnly}
          >
            {companies.map((company) => (
              <option key={company.id} value={company.id}>{company.name}</option>
            ))}
          </select>
        </label>

        <label>
          Position
          <input
            value={position}
            onChange={(event) => setPosition(event.target.value)}
            required
            maxLength={255}
            disabled={readOnly}
          />
        </label>

        <label>
          Source
          <select
            value={source}
            onChange={(event) => setSource(event.target.value as JobSource)}
            disabled={readOnly}
          >
            {jobSources.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>

        <label>
          Location
          <input
            value={location}
            onChange={(event) => setLocation(event.target.value)}
            maxLength={255}
            disabled={readOnly}
          />
        </label>

        <label>
          Work mode
          <select
            value={workMode}
            onChange={(event) => setWorkMode(event.target.value as WorkMode | '')}
            disabled={readOnly}
          >
            <option value="">Not set</option>
            {workModes.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>

        <label>
          Employment type
          <select
            value={employmentType}
            onChange={(event) => setEmploymentType(event.target.value as EmploymentType | '')}
            disabled={readOnly}
          >
            <option value="">Not set</option>
            {employmentTypes.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>

        <label>
          Seniority
          <select
            value={seniorityLevel}
            onChange={(event) => setSeniorityLevel(event.target.value as SeniorityLevel | '')}
            disabled={readOnly}
          >
            <option value="">Not set</option>
            {seniorityLevels.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>

        <label>
          Offer URL
          <input
            type="url"
            value={offerUrl}
            onChange={(event) => setOfferUrl(event.target.value)}
            maxLength={1000}
            disabled={readOnly}
          />
        </label>

        <label>
          Date found
          <input
            type="date"
            value={dateFound}
            onChange={(event) => setDateFound(event.target.value)}
            required
            disabled={readOnly}
          />
        </label>

        <label>
          Date applied
          <input
            type="date"
            value={dateApplied}
            onChange={(event) => setDateApplied(event.target.value)}
            disabled={readOnly}
          />
        </label>

        <label>
          Deadline
          <input
            type="date"
            value={deadline}
            onChange={(event) => setDeadline(event.target.value)}
            disabled={readOnly}
          />
        </label>
      </div>

      <fieldset className="salary-fields">
        <legend>Salary range</legend>
        <div className="form-grid">
          <label>
            Minimum
            <input
              type="number"
              min="0"
              step="0.01"
              value={salaryMin}
              onChange={(event) => setSalaryMin(event.target.value)}
              disabled={readOnly}
            />
          </label>
          <label>
            Maximum
            <input
              type="number"
              min="0"
              step="0.01"
              value={salaryMax}
              onChange={(event) => setSalaryMax(event.target.value)}
              disabled={readOnly}
            />
          </label>
          <label>
            Currency
            <input
              value={currency}
              onChange={(event) => setCurrency(event.target.value)}
              maxLength={3}
              placeholder="PLN"
              disabled={readOnly}
            />
          </label>
          <label>
            Period
            <select
              value={salaryPeriod}
              onChange={(event) => setSalaryPeriod(event.target.value as SalaryPeriod | '')}
              disabled={readOnly}
            >
              <option value="">Not set</option>
              {salaryPeriods.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
          <label>
            Basis
            <select
              value={salaryBasis}
              onChange={(event) => setSalaryBasis(event.target.value as SalaryBasis | '')}
              disabled={readOnly}
            >
              <option value="">Not set</option>
              {salaryBases.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
        </div>
      </fieldset>

      <label>
        Job description
        <textarea
          value={rawDescription}
          onChange={(event) => setRawDescription(event.target.value)}
          rows={8}
          disabled={readOnly}
        />
      </label>

      <div className="form-actions">
        <button type="button" className="button-secondary" onClick={onCancel}>
          Back
        </button>
        {!readOnly && (
          <button type="submit" disabled={submitting}>
            {submitting ? 'Saving...' : 'Save application'}
          </button>
        )}
      </div>
    </form>
  )
}
