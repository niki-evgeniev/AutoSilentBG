package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalTemplateTest {

    @Test
    void publicStaticPagesHaveAbsoluteCanonicalUrls() throws IOException {
        assertThat(template("index.html"))
                .contains("<link rel=\"canonical\" href=\"https://autosilent.bg/\">");
        assertThat(template("contact.html"))
                .contains("<link rel=\"canonical\" href=\"https://autosilent.bg/contact\">");
    }

    @Test
    void productCanonicalAndOpenGraphUrlsUseDynamicProductSlug() throws IOException {
        String template = template("product-details.html");

        assertThat(template)
                .contains("rel=\"canonical\" th:href=\"|https://autosilent.bg/products/${product.url()}|\"")
                .contains("property=\"og:url\" th:content=\"|https://autosilent.bg/products/${product.url()}|\"");
    }

    @Test
    void productDetailsHasDynamicProductJsonLd() throws IOException {
        String template = template("product-details.html");

        assertThat(template)
                .contains("type=\"application/ld+json\" th:inline=\"javascript\"")
                .contains("\"@type\": \"Product\"")
                .contains("\"name\": /*[[${product.displayName()}]]*/")
                .contains("\"sku\": /*[[${product.sku()}]]*/")
                .contains("\"@type\": \"Offer\"")
                .contains("\"priceCurrency\": \"EUR\"")
                .contains("https://schema.org/InStock")
                .contains("https://schema.org/OutOfStock")
                .contains("|https://autosilent.bg/products/${product.url()}|");
    }

    @Test
    void productListingCanonicalSupportsCategoriesAndPagination() throws IOException {
        String template = template("products.html");

        assertThat(template)
                .contains("https://autosilent.bg/products?page=${productPage.number}")
                .contains("https://autosilent.bg/products/category/${selectedCategory.id()}/${selectedCategory.slug()}")
                .contains("?page=${productPage.number}");
    }

    private String template(String name) throws IOException {
        return Files.readString(
                Path.of("src/main/resources/templates", name), StandardCharsets.UTF_8);
    }
}
