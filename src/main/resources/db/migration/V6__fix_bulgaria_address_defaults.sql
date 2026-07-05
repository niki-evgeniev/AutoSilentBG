ALTER TABLE order_addresses
    MODIFY COLUMN country VARCHAR(100) NOT NULL DEFAULT 'България';

ALTER TABLE user_addresses
    MODIFY COLUMN country VARCHAR(100) NOT NULL DEFAULT 'България';
