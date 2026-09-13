package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.CategoryViewDto;
import nevg.autosilent.Models.Dto.ProductViewDto;
import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Models.Dto.SeoDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalTemplateTest {

    @Test
    void publicStaticPagesHaveAbsoluteCanonicalUrls() throws IOException {
        assertThat(template("index.html"))
                .contains("<link rel=\"canonical\" href=\"https://autosilent.bg/\">");
        assertThat(template("contact.html"))
                .contains("<link rel=\"canonical\" href=\"https://autosilent.bg/contact\">");
        assertThat(template("product-return.html"))
                .contains("<link rel=\"canonical\" href=\"https://autosilent.bg/returns\">");
        assertThat(template("delivery.html"))
                .contains("<link rel=\"canonical\" href=\"https://autosilent.bg/delivery\">");
        assertThat(template("payment.html"))
                .contains("<link rel=\"canonical\" href=\"https://autosilent.bg/payment\">");
        assertThat(template("privacy-policy.html"))
                .contains("<link rel=\"canonical\" href=\"https://autosilent.bg/privacy-policy\">");
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
                .contains("\"@type\": \"ItemPage\"")
                .contains("\"@type\": \"BreadcrumbList\"")
                .doesNotContain("[(${product.url()})]");
    }

    @Test
    void productDetailsRendersValidRichJsonLdGraph() throws IOException {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "vibrofiltr-special", "Vibrofiltr \"Special\"", "2 mm", "VF-7",
                "Виброизолация", new BigDecimal("19.90"), "<p>Подробно описание</p>",
                3, 12L, List.of("/images/product.webp", "https://cdn.example.com/second.webp"));
        SeoDto seo = new SeoDto();
        seo.setTitle("SEO заглавие");
        seo.setDescription("SEO описание");
        seo.setImageUrl("/images/seo-product.webp");
        Context context = new Context();
        context.setVariable("product", product);
        context.setVariable("seo", seo);

        String template = template("product-details.html");
        int scriptStart = template.indexOf("<script type=\"application/ld+json\"");
        int scriptEnd = template.indexOf("</script>", scriptStart) + "</script>".length();
        String rendered = new SpringTemplateEngine().process(
                template.substring(scriptStart, scriptEnd), context);
        String json = rendered.substring(rendered.indexOf('>') + 1, rendered.lastIndexOf("</script>"));

        JsonNode graph = new ObjectMapper().readTree(json).path("@graph");
        assertThat(graph.path(0).path("@type").asText()).isEqualTo("Product");
        assertThat(graph.path(0).path("description").asText()).isEqualTo("SEO описание");
        assertThat(graph.path(0).path("image")).hasSize(3);
        assertThat(graph.path(0).path("offers").path("seller").path("@id").asText())
                .isEqualTo("https://autosilent.bg/#organization");
        assertThat(graph.path(1).path("@type").asText()).isEqualTo("ItemPage");
        assertThat(graph.path(2).path("itemListElement")).hasSize(3);
        assertThat(graph.path(3).path("@type").asText()).isEqualTo("ImageObject");
    }

    @Test
    void productCatalogRendersValidDynamicJsonLd() throws IOException {
        ProductViewDto product = new ProductViewDto(
                7L, "vibrofiltr-special", "Vibrofiltr \"Special\"", "2 mm", "VF-7",
                "Виброизолация", new BigDecimal("19.90"), "<p>Описание &amp; характеристики</p>",
                3, "/images/product.webp");
        Context context = new Context();
        CategoryViewDto category = new CategoryViewDto(4L, "Виброизолация");
        PageImpl<ProductViewDto> productPage = new PageImpl<>(
                List.of(product), PageRequest.of(1, 9), 10);
        context.setVariable("selectedCategory", category);
        context.setVariable("products", List.of(product));
        context.setVariable("productPage", productPage);

        String template = template("products.html");
        int scriptStart = template.indexOf("<script type=\"application/ld+json\"");
        int scriptEnd = template.indexOf("</script>", scriptStart) + "</script>".length();
        String rendered = new SpringTemplateEngine().process(
                template.substring(scriptStart, scriptEnd), context);
        String json = rendered.substring(rendered.indexOf('>') + 1, rendered.lastIndexOf("</script>"));

        JsonNode root = new ObjectMapper().readTree(json);
        JsonNode graph = root.path("@graph");
        assertThat(graph.path(0).path("url").asText())
                .isEqualTo("https://autosilent.bg/products/category/4/vibroizolatsiya?page=1");
        assertThat(graph.path(1).path("itemListElement")).hasSize(3);
        assertThat(graph.path(2).path("numberOfItems").asLong()).isEqualTo(10);
        assertThat(graph.path(2).path("itemListElement").path(0).path("position").asInt()).isEqualTo(10);
        assertThat(graph.path(2).path("itemListElement").path(0).path("item").path("name").asText())
                .isEqualTo("Vibrofiltr \"Special\" 2 mm");
        assertThat(graph.path(2).path("itemListElement").path(0).path("item").path("description").asText())
                .isEqualTo("Описание & характеристики");
        assertThat(graph.path(0).path("isPartOf").path("@id").asText())
                .isEqualTo("https://autosilent.bg/#website");
        assertThat(graph.path(3).path("@type").asText()).isEqualTo("ImageObject");
        assertThat(graph.path(4).path("@type").asText()).isEqualTo("Organization");
        assertThat(graph.path(5).path("@type").asText()).isEqualTo("WebSite");
    }

    @Test
    void homePageRendersValidRichJsonLdGraph() throws IOException {
        ProductViewDto product = new ProductViewDto(
                9L, "vibrofiltr-home", "Vibrofiltr", "Gold", "VF-9", "Виброизолация",
                new BigDecimal("29.90"), "<p>Описание &amp; характеристики</p>",
                4, "/images/home-product.webp");
        Context context = new Context();
        context.setVariable("bestSellingProducts", List.of(product));

        String template = template("index.html");
        int scriptStart = template.indexOf("<script type=\"application/ld+json\"");
        int scriptEnd = template.indexOf("</script>", scriptStart) + "</script>".length();
        String rendered = new SpringTemplateEngine().process(
                template.substring(scriptStart, scriptEnd), context);
        String json = rendered.substring(rendered.indexOf('>') + 1, rendered.lastIndexOf("</script>"));

        JsonNode graph = new ObjectMapper().readTree(json).path("@graph");
        assertThat(graph.path(0).path("@type").asText()).isEqualTo("WebPage");
        assertThat(graph.path(1).path("@type").asText()).isEqualTo("WebSite");
        assertThat(graph.path(2).path("@type").asText()).isEqualTo("OnlineStore");
        assertThat(graph.path(3).path("@type").asText()).isEqualTo("ImageObject");
        assertThat(graph.path(4).path("itemListElement")).hasSize(1);
        assertThat(graph.path(4).path("itemListElement").path(0).path("item").path("description").asText())
                .isEqualTo("Описание & характеристики");
        assertThat(graph.path(4).path("itemListElement").path(0).path("item").path("offers")
                .path("priceCurrency").asText()).isEqualTo("EUR");
    }

    @Test
    void structuredDataConditionalBranchesRemainValidJson() throws IOException {
        Context catalogContext = new Context();
        catalogContext.setVariable("selectedCategory", null);
        catalogContext.setVariable("products", List.of());
        catalogContext.setVariable("productPage", new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));
        assertThat(renderJsonLd("products.html", catalogContext).path("@graph").path(2)
                .path("itemListElement")).isEmpty();

        ProductDetailsDto productWithoutImage = new ProductDetailsDto(
                11L, "product-without-image", "Product", null, "SKU-11", "Category",
                new BigDecimal("10.00"), "<p>Plain description</p>", 0, 1L, List.of());
        Context detailsContext = new Context();
        detailsContext.setVariable("product", productWithoutImage);
        detailsContext.setVariable("seo", null);
        JsonNode detailsGraph = renderJsonLd("product-details.html", detailsContext).path("@graph");
        assertThat(detailsGraph.path(0).has("image")).isFalse();
        assertThat(detailsGraph.path(0).path("description").asText()).isEqualTo("Plain description");

        Context homeContext = new Context();
        homeContext.setVariable("bestSellingProducts", List.of());
        assertThat(renderJsonLd("index.html", homeContext).path("@graph").path(4)
                .path("itemListElement")).isEmpty();
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
    void productJsonLdImageRendersWithoutEscapedSlashes() {
        Context context = new Context();
        context.setVariable("imagePath", "/images/products/vibrofiltr.webp");
        String rendered = new SpringTemplateEngine().process(
                "<script type=\"application/ld+json\" th:inline=\"javascript\">" +
                        "{\"image\":[(${imagePath == null ? 'null' : '\"https://autosilent.bg' + imagePath + '\"'})]}" +
                        "</script>",
                context);

        assertThat(rendered)
                .contains("\"image\":\"https://autosilent.bg/images/products/vibrofiltr.webp\"")
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

    private JsonNode renderJsonLd(String templateName, Context context) throws IOException {
        String template = template(templateName);
        int scriptStart = template.indexOf("<script type=\"application/ld+json\"");
        int scriptEnd = template.indexOf("</script>", scriptStart) + "</script>".length();
        String rendered = new SpringTemplateEngine().process(
                template.substring(scriptStart, scriptEnd), context);
        String json = rendered.substring(rendered.indexOf('>') + 1, rendered.lastIndexOf("</script>"));
        return new ObjectMapper().readTree(json);
    }
}
