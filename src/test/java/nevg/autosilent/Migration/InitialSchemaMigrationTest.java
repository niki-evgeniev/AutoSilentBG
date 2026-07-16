package nevg.autosilent.Migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InitialSchemaMigrationTest {

    @Test
    void initialMigrationCreatesCompleteCurrentSchema() throws Exception {
        String url = "jdbc:h2:mem:migration-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load();

        var result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(4);
        assertThat(result.success).isTrue();

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(tables(connection)).contains(
                    "roles", "users", "users_roles", "products", "pictures", "category",
                    "seo_product", "favorites", "orders", "order_items", "order_addresses",
                    "order_status_history", "user_addresses", "ip_addresses", "contact_inquiries",
                    "promo_codes", "flyway_schema_history"
            );
            assertThat(tables(connection)).hasSize(17);
            assertThat(columns(connection, "contact_inquiries")).contains(
                    "id", "uuid", "sender_name", "sender_email", "subject", "message",
                    "ip_address", "created_at", "is_read"
            );
            assertThat(columns(connection, "seo_product")).contains("image_url", "product_id");
            assertThat(columns(connection, "products")).contains("view_count");
            assertThat(columns(connection, "orders")).contains("promo_code", "promo_discount_percent");
            assertThat(columns(connection, "promo_codes")).contains(
                    "code", "discount_percent", "created_by_user_id", "created_at");
            assertThat(columns(connection, "users")).contains("discount_percent", "is_blocked");
            assertThat(uniqueIndexes(connection, "promo_codes")).anySatisfy(columns ->
                    assertThat(columns).containsExactly("code"));
            assertThat(uniqueIndexes(connection, "favorites")).anySatisfy(columns ->
                    assertThat(columns).containsExactlyInAnyOrder("user_id", "product_id"));
            assertThat(uniqueIndexes(connection, "seo_product")).anySatisfy(columns ->
                    assertThat(columns).containsExactly("product_id"));
            assertThat(uniqueIndexes(connection, "order_addresses")).anySatisfy(columns ->
                    assertThat(columns).containsExactly("order_id"));
            assertThat(importedKeys(connection, "favorites")).containsAllEntriesOf(Map.of(
                    "user_id", "users",
                    "product_id", "products"
            ));
            assertThat(importedKeys(connection, "order_items")).containsAllEntriesOf(Map.of(
                    "order_id", "orders",
                    "product_id", "products"
            ));
            assertThat(importedKeys(connection, "promo_codes")).containsEntry("created_by_user_id", "users");
            assertThat(nullableColumns(connection, "contact_inquiries"))
                    .doesNotContain("sender_name", "sender_email", "subject", "message", "ip_address", "created_at", "is_read");
            assertThat(singleValue(connection,
                    "select count(*) from flyway_schema_history where version = '1' and success = true"))
                    .isEqualTo(1);
        }
    }

    private Set<String> tables(Connection connection) throws Exception {
        Set<String> result = new HashSet<>();
        try (ResultSet rows = connection.getMetaData().getTables(null, "public", "%", new String[]{"TABLE"})) {
            while (rows.next()) result.add(rows.getString("TABLE_NAME").toLowerCase());
        }
        return result;
    }

    private Set<String> columns(Connection connection, String table) throws Exception {
        Set<String> result = new HashSet<>();
        try (ResultSet rows = connection.getMetaData().getColumns(null, "public", table, "%")) {
            while (rows.next()) result.add(rows.getString("COLUMN_NAME").toLowerCase());
        }
        return result;
    }

    private Set<Set<String>> uniqueIndexes(Connection connection, String table) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        java.util.Map<String, Set<String>> indexes = new java.util.HashMap<>();
        try (ResultSet rows = metadata.getIndexInfo(null, "public", table, true, false)) {
            while (rows.next()) {
                String index = rows.getString("INDEX_NAME");
                String column = rows.getString("COLUMN_NAME");
                if (index != null && column != null) {
                    indexes.computeIfAbsent(index, ignored -> new HashSet<>()).add(column.toLowerCase());
                }
            }
        }
        return new HashSet<>(indexes.values());
    }

    private Map<String, String> importedKeys(Connection connection, String table) throws Exception {
        Map<String, String> result = new java.util.HashMap<>();
        try (ResultSet rows = connection.getMetaData().getImportedKeys(null, "public", table)) {
            while (rows.next()) {
                result.put(rows.getString("FKCOLUMN_NAME").toLowerCase(),
                        rows.getString("PKTABLE_NAME").toLowerCase());
            }
        }
        return result;
    }

    private Set<String> nullableColumns(Connection connection, String table) throws Exception {
        Set<String> result = new HashSet<>();
        try (ResultSet rows = connection.getMetaData().getColumns(null, "public", table, "%")) {
            while (rows.next()) {
                if (rows.getInt("NULLABLE") == DatabaseMetaData.columnNullable) {
                    result.add(rows.getString("COLUMN_NAME").toLowerCase());
                }
            }
        }
        return result;
    }

    private int singleValue(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet row = statement.executeQuery(sql)) {
            assertThat(row.next()).isTrue();
            return row.getInt(1);
        }
    }
}
