-- notes: free-form notes on the application itself
-- archived: hides finished processes from the default list without deleting them
-- version: optimistic locking, so editing from two devices cannot silently overwrite changes
ALTER TABLE job_application ADD COLUMN notes TEXT;
ALTER TABLE job_application ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE job_application ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_job_application_archived ON job_application (archived);
