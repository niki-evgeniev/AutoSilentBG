CREATE TABLE favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT uk_favorites_uuid UNIQUE (uuid),
    CONSTRAINT uk_favorites_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
