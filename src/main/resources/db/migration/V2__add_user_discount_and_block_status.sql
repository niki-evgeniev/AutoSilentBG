ALTER TABLE users
    ADD COLUMN discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00;

ALTER TABLE users
    ADD COLUMN is_blocked BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD CONSTRAINT chk_users_discount_percent
        CHECK (discount_percent >= 0.00 AND discount_percent <= 100.00);

CREATE INDEX idx_users_blocked ON users (is_blocked);
