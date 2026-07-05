CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    role_type VARCHAR(50) NOT NULL,
    CONSTRAINT uk_roles_uuid UNIQUE (uuid),
    CONSTRAINT uk_roles_role_type UNIQUE (role_type)
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NULL,
    last_name VARCHAR(255) NULL,
    phone_number VARCHAR(255) NULL,
    register_date DATETIME NULL,
    edit_date DATETIME NULL,
    verification_token VARCHAR(255) NULL,
    is_activate BOOLEAN NOT NULL DEFAULT FALSE,
    token_created DATETIME NULL,
    CONSTRAINT uk_users_uuid UNIQUE (uuid),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    name_product VARCHAR(150) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    category VARCHAR(80) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    description TEXT NULL,
    url_link VARCHAR(255) NULL,
    stock INT NULL,
    sold INT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    add_date DATETIME NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT uk_products_uuid UNIQUE (uuid),
    CONSTRAINT uk_products_name UNIQUE (name_product),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE pictures (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    file_name VARCHAR(100) NOT NULL,
    is_main_image BOOLEAN NOT NULL DEFAULT FALSE,
    product_id BIGINT NOT NULL,
    CONSTRAINT uk_pictures_uuid UNIQUE (uuid),
    CONSTRAINT uk_picture_product_filename UNIQUE (product_id, file_name),
    CONSTRAINT fk_pictures_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
