-- next_step_at / next_step_note: the next planned action on an application (interview, task, follow-up).
-- Both are optional and are never changed automatically by status changes.
ALTER TABLE job_application ADD COLUMN next_step_at TIMESTAMPTZ;
ALTER TABLE job_application ADD COLUMN next_step_note VARCHAR(200);

CREATE INDEX idx_job_application_next_step_at ON job_application (next_step_at);
