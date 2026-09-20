package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.CategoryViewDto;
import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Models.Dto.ProductFilterDto;
import nevg.autosilent.Models.Dto.ProductViewDto;
import nevg.autosilent.Models.Dto.SeoDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class HeadSeoTemplateTest {

    private static final String SITE = "https://autosilent.bg";
    private static final CategoryViewDto CATEGORY = new CategoryViewDto(4L, "Виброизолация");
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    @ParameterizedTest
    @ValueSource(strings = {"index.html", "products.html", "product-details.html"})
    void renderedHeadsHaveConsistentMetadataAndConnectedGraphs(String template) throws IOException {
        RenderedHead head = render(template, context(template, Locale.forLanguageTag("bg")));

        assertHeadConsistency(head);
        assertThat(head.meta("property", "og:locale")).isEqualTo("bg_BG");
        assertThat(head.page().path("inLanguage").asText()).startsWith("bg");
        assertThat(head.meta("name", "robots")).contains("index", "follow").doesNotContain("noindex");
    }

    @ParameterizedTest
    @ValueSource(strings = {"index.html"})
    void localeChangesTheLanguageOfHeadMetadata(String template) throws IOException {
        RenderedHead english = render(template, context(template, Locale.ENGLISH));
        RenderedHead bulgarian = render(template, context(template, Locale.forLanguageTag("bg")));

        assertHeadConsistency(english);
        assertThat(english.meta("property", "og:locale")).startsWith("en_");
        assertThat(english.page().path("inLanguage").asText()).startsWith("en");
        assertThat(english.document().title()).isNotEqualTo(bulgarian.document().title());
        assertThat(english.canonical()).contains("lang=en");
        assertThat(english.document().select("link[rel=alternate][hreflang=en]")).hasSize(1);
        assertThat(english.document().select("link[rel=alternate][hreflang=bg]")).hasSize(1);
    }

    @Test
    void englishCatalogUiKeepsBulgarianSeoSemantics() throws IOException {
        RenderedHead head = render("products.html", context("products.html", Locale.ENGLISH));

        assertHeadConsistency(head);
        assertThat(head.canonical()).doesNotContain("lang=");
        assertThat(head.meta("property", "og:locale")).isEqualTo("bg_BG");
        assertThat(head.page().path("inLanguage").asText()).isEqualTo("bg-BG");
        assertThat(head.document().select("link[rel=alternate][hreflang=en]")).isEmpty();
    }

    @Test
    void englishProductUiKeepsBulgarianSeoSemantics() throws IOException {
        RenderedHead head = render("product-details.html",
                context("product-details.html", Locale.ENGLISH));

        assertHeadConsistency(head);
        assertThat(head.canonical()).doesNotContain("lang=");
        assertThat(head.meta("property", "og:locale")).isEqualTo("bg_BG");
        assertThat(head.page().path("inLanguage").asText()).isEqualTo("bg-BG");
        assertThat(head.node("Product").path("inLanguage").asText()).isEqualTo("bg-BG");
        assertThat(head.document().select("link[rel=alternate][hreflang=en]")).isEmpty();
    }

    @Test
    void categoryPaginationHasDistinctTitlesAndGraphIds() throws IOException {
        RenderedHead first = render("products.html", catalogContext(0));
        RenderedHead second = render("products.html", catalogContext(1));

        assertHeadConsistency(first);
        assertHeadConsistency(second);
        assertThat(first.canonical()).isEqualTo(SITE + "/shumoizolaciya/category/4/" + CATEGORY.slug());
        assertThat(second.canonical()).isEqualTo(first.canonical() + "?page=1");
        assertThat(second.document().title()).contains(CATEGORY.name(), "2");
        assertThat(second.document().title()).isNotEqualTo(first.document().title());
        assertThat(second.node("BreadcrumbList").path("@id").asText())
                .isNotEqualTo(first.node("BreadcrumbList").path("@id").asText());
        assertThat(second.node("ItemList").path("@id").asText())
                .isNotEqualTo(first.node("ItemList").path("@id").asText());
        assertThat(second.node("ItemList").path("itemListElement").path(0).path("position").asInt())
                .isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"search", "brand", "model", "minimum", "maximum", "stock", "sort"})
    void filteredSearchAndSortedCatalogsAreNotIndexed(String mode) throws IOException {
        Context context = catalogContext(1);
        String search = mode.equals("search") ? "Vibrofiltr" : null;
        context.setVariable("search", search);
        context.setVariable("filter", new ProductFilterDto(search,
                mode.equals("brand") ? "Vibrofiltr" : null,
                mode.equals("model") ? "Gold" : null,
                mode.equals("minimum") ? BigDecimal.ONE : null,
                mode.equals("maximum") ? BigDecimal.TEN : null,
                mode.equals("stock"), CATEGORY.id()));
        context.setVariable("sortMode", mode.equals("sort") ? "priceAsc" : "default");

        RenderedHead head = render("products.html", context);

        assertHeadConsistency(head);
        assertThat(head.meta("name", "robots")).contains("noindex", "follow");
        assertThat(head.canonical()).doesNotContain("search=", "brand=", "model=", "minPrice=",
                "maxPrice=", "inStock=", "order=");
    }

    @Test
    void catalogHeadToleratesMissingOptionalFilterAttributes() throws IOException {
        Context context = catalogContext(0);
        context.removeVariable("filter");
        context.removeVariable("search");
        context.removeVariable("sortMode");

        RenderedHead head = render("products.html", context);

        assertHeadConsistency(head);
        assertThat(head.meta("name", "robots")).doesNotContain("noindex");
    }

    @ParameterizedTest
    @CsvSource({
            "0, 9, true, false",
            "1, 9, false, false",
            "2, 9, true, true",
            "0, 18, false, true",
            "1, 18, false, true"
    })
    void catalogIndexingExcludesEmptyLaterPagesAndNonstandardPageSizes(
            int page, int size, boolean empty, boolean noindex) throws IOException {
        Context context = catalogContext(page);
        List<ProductViewDto> products = empty ? List.of()
                : List.of(view(7L, "Vibrofiltr", "/images/product.webp", 3));
        context.setVariable("products", products);
        context.setVariable("productPage", new PageImpl<>(products, PageRequest.of(page, size),
                empty ? (page == 0 ? 0 : 10) : 40));

        RenderedHead head = render("products.html", context);

        assertHeadConsistency(head);
        assertThat(head.meta("name", "robots")).contains("follow");
        assertThat(head.meta("name", "robots").contains("noindex")).isEqualTo(noindex);
    }

    @ParameterizedTest
    @ValueSource(strings = {"index.html", "products.html"})
    void emptyListsRenderStrictlyValidJson(String template) throws IOException {
        Context context = context(template, Locale.forLanguageTag("bg"));
        context.setVariable("bestSellingProducts", List.of());
        context.setVariable("products", List.of());
        context.setVariable("productPage", new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));

        RenderedHead head = render(template, context);

        assertHeadConsistency(head);
        assertThat(head.node("ItemList").path("itemListElement")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"index.html", "products.html", "product-details.html"})
    void specialCharactersCannotCloseTheJsonLdScript(String template) throws IOException {
        String name = "Brand \"Special\" \\ model\n</script><meta name=\"injected\" content=\"yes\">";
        Context context = context(template, Locale.forLanguageTag("bg"));
        ProductViewDto product = view(7L, name, "/images/product.webp", 3);
        context.setVariable("product", details(name, List.of("/images/product.webp"), 3));
        context.setVariable("bestSellingProducts", List.of(product, view(8L, "Second", null, 0)));
        context.setVariable("products", List.of(product, view(8L, "Second", null, 0)));
        context.setVariable("productPage", new PageImpl<>(List.of(product), PageRequest.of(0, 9), 2));

        RenderedHead head = render(template, context);
        JsonNode productNode = template.equals("product-details.html") ? head.node("Product")
                : head.node("ItemList").path("itemListElement").path(0).path("item");

        assertThat(head.document().select("meta[name=injected]")).isEmpty();
        assertThat(productNode.path("name").asText()).isEqualTo(name + " 2 mm");
        if (!template.equals("product-details.html")) {
            assertThat(head.node("ItemList").path("itemListElement")).hasSize(2);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "null", "blank"})
    void absentOrBlankProductSeoFallsBackToProductContent(String mode) throws IOException {
        Context context = context("product-details.html", Locale.forLanguageTag("bg"));
        if (!mode.equals("missing")) {
            SeoDto seo = new SeoDto();
            if (mode.equals("blank")) {
                seo.setTitle(" \t ");
                seo.setDescription(" \n ");
                seo.setImageUrl(" ");
            }
            context.setVariable("seo", seo);
        }

        RenderedHead head = render("product-details.html", context);

        assertHeadConsistency(head);
        assertThat(head.document().title()).contains("Vibrofiltr", "2 mm", "AutoSilent.bg");
        assertThat(head.meta("name", "description")).contains("Описание & характеристики")
                .doesNotContain("<p>", "&amp;");
    }

    @Test
    void productSeoOverridesAreSharedByMetadataAndStructuredData() throws IOException {
        Context context = context("product-details.html", Locale.forLanguageTag("bg"));
        SeoDto seo = new SeoDto();
        seo.setTitle("Специално SEO заглавие");
        seo.setDescription("SEO описание с \"кавички\" и & символ.");
        seo.setImageUrl("/images/seo.webp");
        context.setVariable("seo", seo);
        context.setVariable("product", details("Vibrofiltr",
                List.of("/images/main.webp", "https://cdn.example.com/second.webp"), 3));

        RenderedHead head = render("product-details.html", context);

        assertHeadConsistency(head);
        assertThat(head.document().title()).isEqualTo(seo.getTitle());
        assertThat(head.meta("name", "description")).isEqualTo(seo.getDescription());
        assertThat(head.node("Product").path("description").asText()).isEqualTo(seo.getDescription());
        assertThat(head.meta("property", "og:image")).isEqualTo(SITE + "/images/seo.webp");
        assertThat(strings(head.node("Product").path("image")))
                .containsExactly(SITE + "/images/seo.webp", SITE + "/images/main.webp",
                        "https://cdn.example.com/second.webp");
    }

    @Test
    void productWithoutImagesDoesNotAdvertiseAnUnrelatedProductImage() throws IOException {
        Context context = context("product-details.html", Locale.forLanguageTag("bg"));
        context.setVariable("product", details("Vibrofiltr", List.of(), 0));

        RenderedHead head = render("product-details.html", context);

        assertHeadConsistency(head);
        assertThat(head.node("Product").has("image")).isFalse();
        assertOffer(head.node("Product").path("offers"), "OutOfStock");
    }

    @ParameterizedTest
    @CsvSource({
            "/images/product.webp, https://autosilent.bg/images/product.webp",
            "images/product.webp, https://autosilent.bg/images/product.webp",
            "//cdn.example.com/product.webp, https://cdn.example.com/product.webp",
            "https://cdn.example.com/product.webp, https://cdn.example.com/product.webp",
            "http://cdn.example.com/product.webp, http://cdn.example.com/product.webp"
    })
    void productImageUrlsAreAbsoluteInEveryHead(String source, String expected) throws IOException {
        for (String template : List.of("index.html", "products.html", "product-details.html")) {
            Context context = context(template, Locale.forLanguageTag("bg"));
            ProductViewDto product = view(7L, "Vibrofiltr", source, 3);
            context.setVariable("product", details("Vibrofiltr", List.of(source), 3));
            context.setVariable("bestSellingProducts", List.of(product));
            context.setVariable("products", List.of(product));

            RenderedHead head = render(template, context);
            JsonNode productNode = template.equals("product-details.html") ? head.node("Product")
                    : head.node("ItemList").path("itemListElement").path(0).path("item");

            assertThat(strings(productNode.path("image"))).as(template).contains(expected);
            assertOffer(productNode.path("offers"), "InStock");
            if (template.equals("product-details.html")) {
                assertThat(head.meta("property", "og:image")).isEqualTo(expected);
                assertThat(head.meta("name", "twitter:image")).isEqualTo(expected);
                if (expected.startsWith("http://")) {
                    assertThat(head.document().select("meta[property=og:image:secure_url]")).isEmpty();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"data:image/png;base64,AAAA", "javascript:alert(1)"})
    void unsupportedImageSchemesAreOmittedFromProductData(String source) throws IOException {
        for (String template : List.of("index.html", "products.html", "product-details.html")) {
            Context context = context(template, Locale.forLanguageTag("bg"));
            ProductViewDto product = view(7L, "Vibrofiltr", source, 3);
            context.setVariable("product", details("Vibrofiltr", List.of(source), 3));
            context.setVariable("bestSellingProducts", List.of(product));
            context.setVariable("products", List.of(product));

            RenderedHead head = render(template, context);
            JsonNode productNode = template.equals("product-details.html") ? head.node("Product")
                    : head.node("ItemList").path("itemListElement").path(0).path("item");

            assertHeadConsistency(head);
            assertThat(productNode.has("image")).as(template).isFalse();
            assertOffer(productNode.path("offers"), "InStock");
            if (template.equals("product-details.html")) {
                assertThat(head.document().select("meta[property=og:image], meta[name=twitter:image]"))
                        .isEmpty();
            }
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.01"})
    void missingOrNegativePricesDoNotAdvertiseOffers(String amount) throws IOException {
        BigDecimal price = amount == null ? null : new BigDecimal(amount);
        for (String template : List.of("index.html", "products.html", "product-details.html")) {
            Context context = context(template, Locale.forLanguageTag("bg"));
            ProductViewDto product = new ProductViewDto(7L, "product-7", "Vibrofiltr", "2 mm",
                    "SKU-7", CATEGORY.name(), price, "Product description", 3, null);
            context.setVariable("product", new ProductDetailsDto(7L, "product-7", "Vibrofiltr", "2 mm",
                    "SKU-7", CATEGORY.name(), price, "Product description", 3, 12L, List.of()));
            context.setVariable("bestSellingProducts", List.of(product));
            context.setVariable("products", List.of(product));

            RenderedHead head = render(template, context);
            JsonNode productNode = template.equals("product-details.html") ? head.node("Product")
                    : head.node("ItemList").path("itemListElement").path(0).path("item");

            assertHeadConsistency(head);
            assertThat(productNode.has("offers")).as(template).isFalse();
            assertThat(head.document().select(
                    "meta[property=product:price:amount], meta[property=product:price:currency]"))
                    .isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void productGalleryOmitsNullBlankAndDuplicateImagesAndPrioritizesSeoImage(boolean withSeo) throws IOException {
        Context context = context("product-details.html", Locale.forLanguageTag("bg"));
        context.setVariable("product", details("Vibrofiltr",
                Arrays.asList(null, " ", "/images/main.webp", "", "images/second.webp",
                        SITE + "/images/main.webp", " images/second.webp "), 3));
        if (withSeo) {
            SeoDto seo = new SeoDto();
            seo.setImageUrl("/images/second.webp");
            context.setVariable("seo", seo);
        }

        RenderedHead head = render("product-details.html", context);

        String primary = SITE + (withSeo ? "/images/second.webp" : "/images/main.webp");
        String secondary = SITE + (withSeo ? "/images/main.webp" : "/images/second.webp");
        assertHeadConsistency(head);
        assertThat(strings(head.node("Product").path("image")))
                .containsExactly(primary, secondary);
        assertThat(head.meta("property", "og:image")).isEqualTo(primary);
    }

    private void assertHeadConsistency(RenderedHead head) {
        Document document = head.document();
        assertThat(document.select("meta[charset]")).hasSize(1);
        assertThat(document.select("title")).hasSize(1);
        assertThat(document.title()).isNotBlank();
        assertThat(document.select("meta[name=description]")).hasSize(1);
        assertThat(document.select("link[rel=canonical]")).hasSize(1);
        assertThat(head.canonical()).startsWith(SITE + "/");
        assertThat(head.meta("property", "og:url")).isEqualTo(head.canonical());
        assertThat(head.page().path("url").asText()).isEqualTo(head.canonical());
        assertThat(head.meta("property", "og:title")).isEqualTo(document.title());
        assertThat(head.meta("name", "twitter:title")).isEqualTo(document.title());
        assertThat(head.page().path("name").asText()).isEqualTo(document.title());
        assertThat(head.meta("name", "description")).isNotBlank();
        assertThat(head.meta("property", "og:description")).isEqualTo(head.meta("name", "description"));
        assertThat(head.meta("name", "twitter:description")).isEqualTo(head.meta("name", "description"));
        assertThat(head.page().path("description").asText()).isEqualTo(head.meta("name", "description"));
        List<String> ids = StreamSupport.stream(head.graph().spliterator(), false)
                .map(node -> node.path("@id").asText()).toList();
        assertThat(ids).doesNotHaveDuplicates().doesNotContain("");
        for (String property : List.of("isPartOf", "publisher", "primaryImageOfPage", "breadcrumb", "mainEntity", "about")) {
            if (head.page().has(property)) {
                assertThat(ids).as("Resolved page reference: " + property)
                        .contains(head.page().path(property).path("@id").asText());
            }
        }
        assertThat(ids).contains(head.node("WebSite").path("publisher").path("@id").asText());
        document.select("meta[property=og:image], meta[name=twitter:image]").forEach(image -> {
            URI uri = URI.create(image.attr("content"));
            assertThat(uri.getScheme()).isIn("http", "https");
            assertThat(uri.getHost()).isNotBlank();
        });
    }

    private void assertOffer(JsonNode offer, String availability) {
        assertThat(offer.path("price").isNumber()).isTrue();
        assertThat(offer.path("price").decimalValue()).isEqualByComparingTo("19.90");
        assertThat(offer.path("priceCurrency").asText()).isEqualTo("EUR");
        assertThat(offer.path("availability").asText()).isEqualTo("https://schema.org/" + availability);
    }

    private Context context(String template, Locale locale) {
        Context context = catalogContext(0);
        context.setLocale(locale);
        context.setVariable("selectedCategory", null);
        context.setVariable("bestSellingProducts", List.of(view(7L, "Vibrofiltr", "/images/product.webp", 3)));
        context.setVariable("product", details("Vibrofiltr", List.of("/images/product.webp"), 3));
        return context;
    }

    private Context catalogContext(int page) {
        Context context = new Context(Locale.forLanguageTag("bg"));
        List<ProductViewDto> products = List.of(view(7L, "Vibrofiltr", "/images/product.webp", 3));
        context.setVariable("selectedCategory", CATEGORY);
        context.setVariable("products", products);
        context.setVariable("productPage", new PageImpl<>(products, PageRequest.of(page, 9), 20));
        context.setVariable("filter", new ProductFilterDto(null, null, null, null, null, false, CATEGORY.id()));
        context.setVariable("search", "");
        context.setVariable("sortMode", "default");
        return context;
    }

    private ProductViewDto view(long id, String name, String image, int stock) {
        return new ProductViewDto(id, "product-" + id, name, "2 mm", "SKU-" + id, CATEGORY.name(),
                new BigDecimal("19.90"), "<p>Описание &amp; характеристики</p>", stock, image);
    }

    private ProductDetailsDto details(String name, List<String> images, int stock) {
        return new ProductDetailsDto(7L, "product-7", name, "2 mm", "SKU-7", CATEGORY.name(),
                new BigDecimal("19.90"), "<p>Описание &amp; характеристики</p>", stock, 12L, images);
    }

    private RenderedHead render(String name, Context context) throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates", name), StandardCharsets.UTF_8);
        String fullHead = template.substring(template.indexOf("<head"), template.indexOf("</head>") + 7);
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Document document = Jsoup.parse(engine.process(fullHead, context));
        assertThat(document.select("script[type=application/ld+json]")).hasSize(1);
        JsonNode root = JSON.readTree(document.selectFirst("script[type=application/ld+json]").data());
        assertThat(root.path("@context").asText()).isEqualTo("https://schema.org");
        assertThat(root.path("@graph").isArray()).isTrue();
        return new RenderedHead(document, root.path("@graph"));
    }

    private List<String> strings(JsonNode value) {
        if (value.isString()) {
            return List.of(value.asText());
        }
        List<String> values = new ArrayList<>();
        value.forEach(item -> values.add(item.asText()));
        return values;
    }

    private record RenderedHead(Document document, JsonNode graph) {
        String meta(String attribute, String name) {
            var elements = document.select("meta[" + attribute + "=" + name + "]");
            assertThat(elements).as("Unique meta " + name).hasSize(1);
            return elements.first().attr("content");
        }

        String canonical() {
            return document.selectFirst("link[rel=canonical]").attr("href");
        }

        JsonNode node(String type) {
            return StreamSupport.stream(graph.spliterator(), false)
                    .filter(node -> node.path("@type").asText().equals(type))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing graph type " + type));
        }

        JsonNode page() {
            return StreamSupport.stream(graph.spliterator(), false)
                    .filter(node -> List.of("WebPage", "CollectionPage", "ItemPage")
                            .contains(node.path("@type").asText()))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing page graph node"));
        }
    }
}
