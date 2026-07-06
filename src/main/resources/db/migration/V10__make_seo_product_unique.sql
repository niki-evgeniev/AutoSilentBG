DELETE FROM seo_product WHERE product_id IS NULL;

DELETE duplicate_seo
FROM seo_product duplicate_seo
JOIN seo_product retained_seo
  ON duplicate_seo.product_id = retained_seo.product_id
 AND duplicate_seo.id > retained_seo.id;

ALTER TABLE seo_product
    MODIFY COLUMN product_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_seo_product_product UNIQUE (product_id);
