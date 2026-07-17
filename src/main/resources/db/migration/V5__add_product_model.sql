ALTER TABLE products
    ADD COLUMN model VARCHAR(150) NULL AFTER name_product;

ALTER TABLE products
    DROP INDEX uk_products_name;

ALTER TABLE products
    ADD CONSTRAINT uk_products_brand_model UNIQUE (name_product, model);
