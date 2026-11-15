-- Storage metadata is nullable to keep existing metadata-only documents valid.
ALTER TABLE document
    ADD COLUMN object_key VARCHAR(512),
    ADD COLUMN content_type VARCHAR(255),
    ADD COLUMN file_size BIGINT;

CREATE UNIQUE INDEX uq_document_object_key
    ON document (object_key)
    WHERE object_key IS NOT NULL;
