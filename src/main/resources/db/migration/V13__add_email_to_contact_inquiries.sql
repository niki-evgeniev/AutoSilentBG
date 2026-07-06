ALTER TABLE contact_inquiries
    ADD COLUMN sender_email VARCHAR(254) NULL AFTER sender_name;

UPDATE contact_inquiries
SET sender_email = CONCAT('unknown-', id, '@invalid.local')
WHERE sender_email IS NULL;

ALTER TABLE contact_inquiries
    MODIFY COLUMN sender_email VARCHAR(254) NOT NULL;
