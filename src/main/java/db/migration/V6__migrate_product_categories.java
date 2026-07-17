package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class V6__migrate_product_categories extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String database = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE products ADD COLUMN category_id BIGINT NULL");
            if (database.contains("mysql")) {
                statement.execute("ALTER TABLE category DROP FOREIGN KEY fk_category_product");
            } else {
                statement.execute("ALTER TABLE category DROP CONSTRAINT fk_category_product");
            }
            statement.execute("ALTER TABLE category DROP COLUMN product_id");
            statement.execute("DELETE FROM category WHERE category IS NULL OR TRIM(category) = ''");
        }

        Map<String, Long> categoryIds = loadAndDeduplicateCategories(connection);
        migrateProductCategories(connection, categoryIds);

        try (Statement statement = connection.createStatement()) {
            if (database.contains("mysql")) {
                statement.execute("ALTER TABLE products MODIFY COLUMN category_id BIGINT NOT NULL");
            } else {
                statement.execute("ALTER TABLE products ALTER COLUMN category_id BIGINT NOT NULL");
            }
            statement.execute("ALTER TABLE category ADD CONSTRAINT uk_category_name UNIQUE (category)");
            statement.execute("ALTER TABLE products ADD CONSTRAINT fk_products_category " +
                    "FOREIGN KEY (category_id) REFERENCES category(id)");
            statement.execute("ALTER TABLE products DROP COLUMN category");
        }
    }

    private Map<String, Long> loadAndDeduplicateCategories(Connection connection) throws Exception {
        Map<String, Long> ids = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, category FROM category ORDER BY id")) {
            while (rows.next()) {
                long id = rows.getLong("id");
                String name = rows.getString("category").trim();
                String key = name.toLowerCase(Locale.ROOT);
                Long existingId = ids.putIfAbsent(key, id);
                if (existingId != null) {
                    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM category WHERE id = ?")) {
                        delete.setLong(1, id);
                        delete.executeUpdate();
                    }
                }
            }
        }
        return ids;
    }

    private void migrateProductCategories(Connection connection, Map<String, Long> categoryIds) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet products = statement.executeQuery("SELECT id, category FROM products ORDER BY id")) {
            while (products.next()) {
                long productId = products.getLong("id");
                String categoryName = products.getString("category").trim();
                String key = categoryName.toLowerCase(Locale.ROOT);
                Long categoryId = categoryIds.get(key);
                if (categoryId == null) {
                    categoryId = insertCategory(connection, categoryName);
                    categoryIds.put(key, categoryId);
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE products SET category_id = ? WHERE id = ?")) {
                    update.setLong(1, categoryId);
                    update.setLong(2, productId);
                    update.executeUpdate();
                }
            }
        }
    }

    private long insertCategory(Connection connection, String name) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO category (uuid, category) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setBytes(1, uuidBytes(UUID.randomUUID()));
            insert.setString(2, name);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Не може да бъде създадена категория: " + name);
                }
                return keys.getLong(1);
            }
        }
    }

    private byte[] uuidBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }
}
