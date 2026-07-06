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
    image_url TEXT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT uk_seo_product_uuid UNIQUE (uuid),
    CONSTRAINT uk_seo_product_product UNIQUE (product_id),
    CONSTRAINT fk_seo_product_product FOREIGN KEY (product_id) REFERENCES products(id)
);

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

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    user_id BIGINT NULL,
    customer_first_name VARCHAR(100) NOT NULL,
    customer_last_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NULL,
    customer_phone VARCHAR(50) NOT NULL,
    delivery_type VARCHAR(50) NOT NULL,
    delivery_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    subtotal_price DECIMAL(10,2) NOT NULL,
    discount_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_price DECIMAL(10,2) NOT NULL,
    customer_note TEXT NULL,
    admin_note TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT uk_orders_uuid UNIQUE (uuid),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    product_image_url VARCHAR(500) NULL,
    CONSTRAINT uk_order_items_uuid UNIQUE (uuid),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE order_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    order_id BIGINT NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'България',
    city VARCHAR(100) NOT NULL,
    postcode VARCHAR(20) NULL,
    address_line VARCHAR(255) NULL,
    courier_office_name VARCHAR(255) NULL,
    courier_office_code VARCHAR(100) NULL,
    CONSTRAINT uk_order_addresses_uuid UNIQUE (uuid),
    CONSTRAINT uk_order_addresses_order UNIQUE (order_id),
    CONSTRAINT fk_order_addresses_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE order_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    order_id BIGINT NOT NULL,
    old_status VARCHAR(50) NULL,
    new_status VARCHAR(50) NOT NULL,
    changed_by_user_id BIGINT NULL,
    note TEXT NULL,
    changed_at DATETIME NOT NULL,
    CONSTRAINT uk_order_status_history_uuid UNIQUE (uuid),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);

CREATE TABLE user_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'България',
    city VARCHAR(100) NOT NULL,
    postcode VARCHAR(20) NULL,
    address_line VARCHAR(255) NULL,
    courier_type VARCHAR(50) NULL,
    courier_office_name VARCHAR(255) NULL,
    courier_office_code VARCHAR(100) NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT uk_user_addresses_uuid UNIQUE (uuid),
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

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

CREATE TABLE contact_inquiries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid BINARY(16) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    sender_email VARCHAR(254) NOT NULL,
    subject VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    created_at DATETIME NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_contact_inquiries_uuid UNIQUE (uuid),
    INDEX idx_contact_inquiries_created_at (created_at),
    INDEX idx_contact_inquiries_ip_address (ip_address),
    INDEX idx_contact_inquiries_read_created (is_read, created_at)
);
