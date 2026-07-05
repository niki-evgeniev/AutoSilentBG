CREATE TABLE ip_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    first_seen DATETIME(6) NOT NULL,
    last_seen DATETIME(6) NOT NULL,
    count_visits BIGINT NOT NULL DEFAULT 0,
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    banned_until DATETIME(6) NULL,
    user_id BIGINT NULL,
    CONSTRAINT uk_ip_addresses_uuid UNIQUE (uuid),
    CONSTRAINT uk_ip_addresses_address UNIQUE (ip_address),
    CONSTRAINT fk_ip_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_ip_addresses_banned (is_banned, banned_until),
    INDEX idx_ip_addresses_user (user_id)
);
