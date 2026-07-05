CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    category VARCHAR(255) NULL,
    product_id BIGINT NULL,
    CONSTRAINT uk_category_uuid UNIQUE (uuid),
    CONSTRAINT fk_category_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE seo_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    keywords TEXT NULL,
    title TEXT NULL,
    description TEXT NULL,
    imageUrl TEXT NULL,
    product_id BIGINT NULL,
    CONSTRAINT uk_seo_product_uuid UNIQUE (uuid),
    CONSTRAINT fk_seo_product_product FOREIGN KEY (product_id) REFERENCES products(id)
);
