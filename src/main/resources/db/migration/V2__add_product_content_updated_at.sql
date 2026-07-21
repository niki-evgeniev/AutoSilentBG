ALTER TABLE products
    ADD COLUMN content_updated_at DATETIME NULL;

UPDATE products
SET content_updated_at = add_date
WHERE content_updated_at IS NULL;
