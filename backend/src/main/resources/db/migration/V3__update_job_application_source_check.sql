-- Usunięcie starego ograniczenia (nazwa pochodzi ze skryptu V1)
ALTER TABLE job_application DROP CONSTRAINT ck_job_application_source;

-- Dodanie nowego ograniczenia z rozszerzoną listą wartości
ALTER TABLE job_application ADD CONSTRAINT ck_job_application_source CHECK (source IN (
    'PRACUJ_PL', 'LINKEDIN', 'THE_PROTOCOL', 'JUST_JOIN_IT', 'OTHER'
));