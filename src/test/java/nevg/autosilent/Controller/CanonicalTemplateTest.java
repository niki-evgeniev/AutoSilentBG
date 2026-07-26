package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

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
                .contains("'InStock'")
                .contains("'OutOfStock'")
                .contains("\"url\": \"https://autosilent.bg/products/[(${product.url()})]\"");
    }

    @Test
    void productJsonLdUrlRendersWithoutEscapedSlashes() {
        Context context = new Context();
        context.setVariable("slug", "vibrofiltr-1-5");
        String rendered = new SpringTemplateEngine().process(
                "<script type=\"application/ld+json\" th:inline=\"javascript\">" +
                        "{\"url\":\"https://autosilent.bg/products/[(${slug})]\"}</script>",
                context);

        assertThat(rendered)
                .contains("\"url\":\"https://autosilent.bg/products/vibrofiltr-1-5\"")
                .doesNotContain("\\/");
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
