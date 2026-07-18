package db.migration;

import nevg.autosilent.Utility.ProductSlugGenerator;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class V7__add_product_seo_urls extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Set<String> usedUrls = new HashSet<>();

        try (Statement statement = connection.createStatement();
             ResultSet products = statement.executeQuery(
                     "SELECT id, name_product, model FROM products ORDER BY id")) {
            while (products.next()) {
                long id = products.getLong("id");
                String name = products.getString("name_product");
                String model = products.getString("model");
                String displayName = model == null || model.isBlank() ? name : name + " " + model;
                String url = uniqueUrl(ProductSlugGenerator.toSlug(displayName), usedUrls);

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE products SET url_link = ? WHERE id = ?")) {
                    update.setString(1, url);
                    update.setLong(2, id);
                    update.executeUpdate();
                }
            }
        }

        String database = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        try (Statement statement = connection.createStatement()) {
            if (database.contains("mysql")) {
                statement.execute("ALTER TABLE products MODIFY COLUMN url_link VARCHAR(180) NOT NULL");
            } else {
                statement.execute("ALTER TABLE products ALTER COLUMN url_link VARCHAR(180) NOT NULL");
            }
            statement.execute("ALTER TABLE products ADD CONSTRAINT uk_products_url UNIQUE (url_link)");
        }
    }

    private String uniqueUrl(String base, Set<String> usedUrls) {
        String candidate = base;
        int suffix = 2;
        while (!usedUrls.add(candidate)) {
            String suffixText = "-" + suffix++;
            int baseLength = Math.min(base.length(), 180 - suffixText.length());
            candidate = base.substring(0, baseLength).replaceAll("-+$", "") + suffixText;
        }
        return candidate;
    }
}
