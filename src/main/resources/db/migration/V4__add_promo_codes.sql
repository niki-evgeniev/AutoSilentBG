CREATE TABLE promo_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    code VARCHAR(40) NOT NULL,
    discount_percent INT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_promo_codes_uuid UNIQUE (uuid),
    CONSTRAINT uk_promo_codes_code UNIQUE (code),
    CONSTRAINT fk_promo_codes_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_promo_codes_discount CHECK (discount_percent IN (5, 10, 15, 20, 25, 30))
);

ALTER TABLE orders
    ADD COLUMN promo_code VARCHAR(40) NULL;

ALTER TABLE orders
    ADD COLUMN promo_discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00;
