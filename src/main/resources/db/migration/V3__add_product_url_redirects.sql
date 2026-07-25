CREATE TABLE product_url_redirects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    old_url VARCHAR(180) NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT uk_product_url_redirects_uuid UNIQUE (uuid),
    CONSTRAINT uk_product_url_redirects_old_url UNIQUE (old_url),
    CONSTRAINT fk_product_url_redirects_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_url_redirects_product
    ON product_url_redirects (product_id);
