ALTER TABLE orders ADD COLUMN uuid BINARY(16) NULL;
UPDATE orders SET uuid = UNHEX(REPLACE(UUID(), '-', '')) WHERE uuid IS NULL;
ALTER TABLE orders MODIFY COLUMN uuid BINARY(16) NOT NULL;
ALTER TABLE orders ADD CONSTRAINT uk_orders_uuid UNIQUE (uuid);

ALTER TABLE order_items ADD COLUMN uuid BINARY(16) NULL;
UPDATE order_items SET uuid = UNHEX(REPLACE(UUID(), '-', '')) WHERE uuid IS NULL;
ALTER TABLE order_items MODIFY COLUMN uuid BINARY(16) NOT NULL;
ALTER TABLE order_items ADD CONSTRAINT uk_order_items_uuid UNIQUE (uuid);

ALTER TABLE order_addresses ADD COLUMN uuid BINARY(16) NULL;
UPDATE order_addresses SET uuid = UNHEX(REPLACE(UUID(), '-', '')) WHERE uuid IS NULL;
ALTER TABLE order_addresses MODIFY COLUMN uuid BINARY(16) NOT NULL;
ALTER TABLE order_addresses ADD CONSTRAINT uk_order_addresses_uuid UNIQUE (uuid);

ALTER TABLE order_status_history ADD COLUMN uuid BINARY(16) NULL;
UPDATE order_status_history SET uuid = UNHEX(REPLACE(UUID(), '-', '')) WHERE uuid IS NULL;
ALTER TABLE order_status_history MODIFY COLUMN uuid BINARY(16) NOT NULL;
ALTER TABLE order_status_history ADD CONSTRAINT uk_order_status_history_uuid UNIQUE (uuid);

ALTER TABLE user_addresses ADD COLUMN uuid BINARY(16) NULL;
UPDATE user_addresses SET uuid = UNHEX(REPLACE(UUID(), '-', '')) WHERE uuid IS NULL;
ALTER TABLE user_addresses MODIFY COLUMN uuid BINARY(16) NOT NULL;
ALTER TABLE user_addresses ADD CONSTRAINT uk_user_addresses_uuid UNIQUE (uuid);
