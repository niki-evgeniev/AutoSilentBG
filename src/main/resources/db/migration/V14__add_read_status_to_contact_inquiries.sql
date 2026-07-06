ALTER TABLE contact_inquiries
    ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_contact_inquiries_read_created
    ON contact_inquiries (is_read, created_at);
