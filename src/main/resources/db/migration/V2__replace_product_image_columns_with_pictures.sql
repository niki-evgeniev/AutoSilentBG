DROP TABLE IF EXISTS product_additional_images;

SET @drop_main_image_path = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'products'
              AND column_name = 'main_image_path'
        ),
        'ALTER TABLE products DROP COLUMN main_image_path',
        'SELECT 1'
    )
);

PREPARE drop_main_image_path_statement FROM @drop_main_image_path;
EXECUTE drop_main_image_path_statement;
DEALLOCATE PREPARE drop_main_image_path_statement;
