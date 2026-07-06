CREATE TABLE contact_inquiries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    subject VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_contact_inquiries_uuid UNIQUE (uuid),
    INDEX idx_contact_inquiries_created_at (created_at),
    INDEX idx_contact_inquiries_ip_address (ip_address)
);
