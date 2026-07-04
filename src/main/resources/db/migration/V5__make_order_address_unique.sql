ALTER TABLE order_addresses
    ADD CONSTRAINT uk_order_addresses_order UNIQUE (order_id);
