package nevg.autosilent;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AutoSilentApplicationTests {

    @Autowired
    private WebApplicationContext context;

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
    }

    @Test
    void onlyAdminCanSeeAndDeleteAnOrderWithAllDependentRecordsAndValidCsrf() throws Exception {
        var orders = context.getBean(nevg.autosilent.Repository.OrderRepository.class);
        var items = context.getBean(nevg.autosilent.Repository.OrderItemRepository.class);
        var history = context.getBean(nevg.autosilent.Repository.OrderStatusHistoryRepository.class);
        var addresses = context.getBean(nevg.autosilent.Repository.OrderAddressRepository.class);
        var order = new nevg.autosilent.Models.Entity.OrderEntity();
        order.setOrderNumber("DELETE-" + java.util.UUID.randomUUID());
        order.setCustomerFirstName("Delete"); order.setCustomerLastName("Test"); order.setCustomerPhone("0888123456");
        order.setDeliveryType(nevg.autosilent.Models.Enums.DeliveryType.ECONT_ADDRESS);
        order.setPaymentMethod(nevg.autosilent.Models.Enums.PaymentMethod.CASH_ON_DELIVERY);
        order.setPaymentStatus(nevg.autosilent.Models.Enums.PaymentStatus.PENDING);
        order.setOrderStatus(nevg.autosilent.Models.Enums.OrderStatus.NEW);
        order.setSubtotalPrice(java.math.BigDecimal.TEN); order.setTotalPrice(java.math.BigDecimal.TEN);
        orders.saveAndFlush(order);
        var item = new nevg.autosilent.Models.Entity.OrderItemEntity();
        item.setOrder(order); item.setProductName("Delete test"); item.setQuantity(1);
        item.setUnitPrice(java.math.BigDecimal.TEN); item.setTotalPrice(java.math.BigDecimal.TEN); items.saveAndFlush(item);
        var entry = new nevg.autosilent.Models.Entity.OrderStatusHistoryEntity();
        entry.setOrder(order); entry.setNewStatus(nevg.autosilent.Models.Enums.OrderStatus.NEW); history.saveAndFlush(entry);
        var address = new nevg.autosilent.Models.Entity.OrderAddressEntity();
        address.setOrder(order); address.setCity("Test"); address.setAddressLine("Test address"); addresses.saveAndFlush(address);

        var mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
        for (String role : List.of("MODERATOR", "ADMIN")) {
            var session = new org.springframework.mock.web.MockHttpSession();
            var roles = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
            var principal = new nevg.autosilent.Models.Security.ShopUserDetails("test@example.com", "test", "Test", roles);
            var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal, "test", roles);
            session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new org.springframework.security.core.context.SecurityContextImpl(auth));
            var pageResult = mvc.perform(get("/admin/orders").session(session)).andReturn();
            assertThat(pageResult.getResponse().getStatus()).isEqualTo(200);
            var page = Jsoup.parse(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
            var token = (org.springframework.security.web.csrf.CsrfToken) pageResult.getRequest()
                    .getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
            if ("MODERATOR".equals(role)) {
                assertThat(page.select(".order-delete-form")).isEmpty();
                var denied = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/orders/" + order.getId() + "/delete").session(session)
                        .param(token.getParameterName(), token.getToken())).andReturn().getResponse();
                assertThat(denied.getStatus()).isEqualTo(403);
                assertThat(orders.existsById(order.getId())).isTrue();
            } else {
                assertThat(page.selectFirst("form[action='/admin/orders/" + order.getId() + "/delete'] button")
                        .hasClass("btn-outline-danger")).isTrue();
                var noCsrf = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/orders/" + order.getId() + "/delete").session(session)).andReturn().getResponse();
                assertThat(noCsrf.getStatus()).isEqualTo(403);
                var deleted = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/orders/" + order.getId() + "/delete").session(session)
                        .param(token.getParameterName(), token.getToken())).andReturn().getResponse();
                assertThat(deleted.getStatus()).isEqualTo(302);
                assertThat(deleted.getRedirectedUrl()).isEqualTo("/admin/orders");
            }
        }
        assertThat(orders.existsById(order.getId())).isFalse();
        assertThat(items.findByOrderId(order.getId())).isEmpty();
        assertThat(history.findByOrderIdOrderByChangedAtDesc(order.getId())).isEmpty();
        assertThat(addresses.findByOrderId(order.getId())).isEmpty();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void adminInformationRendersEditableStockAndSavesOnlySubmittedProducts() throws Exception {
        var categories = context.getBean(nevg.autosilent.Repository.CategoryRepository.class);
        var products = context.getBean(nevg.autosilent.Repository.ProductRepository.class);
        var category = new nevg.autosilent.Models.Entity.Category();
        category.setCategory("Stock test " + java.util.UUID.randomUUID()); categories.save(category);
        var product = new nevg.autosilent.Models.Entity.Product();
        product.setNameProduct("Stock test"); product.setSku("STOCK-" + java.util.UUID.randomUUID());
        product.setUrl("stock-" + java.util.UUID.randomUUID()); product.setCategory(category);
        product.setUser(context.getBean(nevg.autosilent.Repository.UserRepository.class).findAll().getFirst());
        product.setPrice(new java.math.BigDecimal("10")); product.setStock(5); products.saveAndFlush(product);
        var roles = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
        var principal = new nevg.autosilent.Models.Security.ShopUserDetails("admin@example.com", "test", "Admin", roles);
        var authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal, "test", roles);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            var response = mvc.perform(get("/admin/settings/information").principal(authentication)).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            var page = Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
            var input = page.selectFirst("#stock-" + product.getId());
            assertThat(input.attr("value")).isEqualTo("5");
            assertThat(input.attr("min")).isEqualTo("0");
            assertThat(page.selectFirst("#stockUpdateButton").text()).isEqualTo("Обнови наличност");
            String body = "{\"changes\":[{\"productId\":" + product.getId() + ",\"originalStock\":5,\"stock\":12}]}";
            var saved = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/admin/settings/information/stock").principal(authentication)
                    .contentType("application/json").content(body)).andReturn().getResponse();
            assertThat(saved.getStatus()).isEqualTo(200);
            assertThat(saved.getContentAsString()).contains("\"updated\":1");
            assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(12);
            var stale = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/admin/settings/information/stock").principal(authentication)
                    .contentType("application/json").content(body)).andReturn().getResponse();
            assertThat(stale.getStatus()).isEqualTo(409);
            assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(12);
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    @Test
    void bothCheckoutStepsRenderForGuestsWithCsrfAndEcontOnly() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String path : List.of("/orders/checkout", "/orders/checkout/delivery")) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertThat(response.statusCode()).isEqualTo(200);
                var page = Jsoup.parse(response.body());
                assertThat(page.selectFirst("meta[name=_csrf]").attr("content")).isNotBlank();
                String script = page.selectFirst("script[src^=/js/shop-]").attr("src");
                assertThat(script).matches("/js/shop-[a-f0-9]{32}\\.js");
                assertThat(script).isEqualTo("/js/shop-" + org.springframework.util.DigestUtils.md5DigestAsHex(
                        Files.readAllBytes(Path.of("src/main/resources/static/js/shop.js"))) + ".js");
                String css = page.selectFirst("link[href^=/css/shop-]").attr("href");
                assertThat(css).matches("/css/shop-[a-f0-9]{32}\\.css");
                var javascript = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + script)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertThat(javascript.statusCode()).isEqualTo(200);
                assertThat(javascript.body()).isEqualTo(Files.readString(Path.of("src/main/resources/static/js/shop.js"), StandardCharsets.UTF_8));
                if (path.endsWith("delivery")) {
                    assertThat(page.select("datalist")).isEmpty();
                    assertThat(page.selectFirst("#deliveryCity").attr("role")).isEqualTo("combobox");
                    assertThat(page.selectFirst("#deliveryCities").attr("role")).isEqualTo("listbox");
                    assertThat(page.selectFirst("script[src^=/js/city-picker-]")).isNotNull();
                    assertThat(page.selectFirst("#deliveryForm")).isNotNull();
                    assertThat(page.selectFirst(".courier-choice").text()).contains("ЕКОНТ");
                    assertThat(page.selectFirst("#deliveryConfirm").hasAttr("disabled")).isTrue();
                    assertThat(page.text()).doesNotContain("Speedy", "Спиди");
                } else {
                    assertThat(page.selectFirst("#cartCheckoutButton").text()).isEqualTo("Към доставка");
                }
            }
        }
    }

    @Test
    void legacyCheckoutPayloadGetsActionableErrorWithoutCreatingAnOrder() throws Exception {
        var orders = context.getBean(nevg.autosilent.Repository.OrderRepository.class);
        long count = orders.count();
        String payload = """
                {"items":[{"productId":1,"quantity":1}],"firstName":"Ivan","lastName":"Ivanov",
                "email":"ivan@example.com","phone":"0888123456","customerNote":"","promoCode":""}
                """;
        var response = MockMvcBuilders.webAppContextSetup(context).build().perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders/cart")
                        .contentType("application/json").content(payload)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("Към доставка").doesNotContain("Bad Request");
        assertThat(orders.count()).isEqualTo(count);
    }

    @Test
    void productsPageRendersWithCombinedFilters() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        var defaultResponse = mockMvc.perform(get("/shumoizolaciya"))
                .andReturn()
                .getResponse();
        var response = mockMvc.perform(get("/shumoizolaciya")
                        .param("brand", "Missing brand")
                        .param("model", "Missing model")
                        .param("minPrice", "10")
                        .param("maxPrice", "100")
                        .param("inStock", "true"))
                .andReturn()
                .getResponse();

        assertThat(defaultResponse.getStatus()).isEqualTo(200);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString())
                .contains("name=\"brand\"")
                .contains("name=\"model\"")
                .contains("name=\"minPrice\"")
                .contains("name=\"maxPrice\"")
                .contains("name=\"inStock\"");
    }

    @Test
    void catalogPageZeroRedirectsWithoutDoubleEncodingAndNegativePageIsMissing() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        var firstPage = mockMvc.perform(get("/shumoizolaciya")
                        .param("page", "0").param("search", "door panel"))
                .andReturn().getResponse();
        var negativePage = mockMvc.perform(get("/shumoizolaciya").param("page", "-1"))
                .andReturn().getResponse();

        assertThat(firstPage.getStatus()).isEqualTo(301);
        assertThat(firstPage.getHeader("Location"))
                .isEqualTo("/shumoizolaciya?search=door%20panel");
        assertThat(negativePage.getStatus()).isEqualTo(404);
    }

    @Test
    void sitemapIsXmlWithoutVisualization() throws Exception {
        Path sitemapFile = Path.of("build", "test-sitemap", "sitemap.xml");
        assertThat(Files.exists(sitemapFile)).isTrue();

        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/sitemap.xml")).GET().build();
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("application/xml"));
        assertThat(response.headers().firstValue("Cache-Control")).hasValueSatisfying(
                cacheControl -> assertThat(cacheControl).contains("no-cache"));
        assertThat(response.body())
                .isEqualTo(Files.readString(sitemapFile, StandardCharsets.UTF_8));
        assertThat(response.body())
                .contains("<urlset")
                .doesNotContain("xml-stylesheet", "xmlns:xhtml", "xhtml:link", "<html", "<style");
    }

    @Test
    void robotsTxtIsServedFromStaticFile() throws Exception {
        Path robotsFile = Path.of("src", "main", "resources", "static", "robots.txt");
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/robots.txt")).GET().build();
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("text/plain"));
        assertThat(response.body()).isEqualTo(Files.readString(robotsFile, StandardCharsets.UTF_8))
                .contains("Sitemap: https://autosilent.bg/sitemap.xml");
    }

    @Test
    void localizedPagesKeepReciprocalHreflangInHtml() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String path : List.of("/", "/contact")) {
                String bulgarianUrl = "https://autosilent.bg" + path;
                String englishUrl = bulgarianUrl + "?lang=en";
                for (String languageSuffix : List.of("", "?lang=en")) {
                    HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://localhost:" + port + path + languageSuffix)).GET().build();
                    HttpResponse<String> response = client.send(request,
                            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    assertThat(response.statusCode()).isEqualTo(200);
                    var head = Jsoup.parse(response.body()).head();
                    assertThat(head.selectFirst("link[rel=canonical]").attr("href"))
                            .isEqualTo(languageSuffix.isEmpty() ? bulgarianUrl : englishUrl);
                    assertThat(head.selectFirst("link[rel=alternate][hreflang=bg]").attr("href"))
                            .isEqualTo(bulgarianUrl);
                    assertThat(head.selectFirst("link[rel=alternate][hreflang=en]").attr("href"))
                            .isEqualTo(englishUrl);
                    assertThat(head.selectFirst("link[rel=alternate][hreflang=x-default]").attr("href"))
                            .isEqualTo(bulgarianUrl);
                }
            }
        }
    }

    @Test
    void kitCatalogLanguageQueryRendersEnglishInterface() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/shumoizolaciya/kit?lang=en"))
                .GET().build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(200);
            var page = Jsoup.parse(response.body());
            assertThat(page.selectFirst("html").attr("lang")).isEqualTo("en");
            assertThat(page.selectFirst(".section-title h1").text()).isEqualTo("Kits");
            assertThat(page.selectFirst("link[rel=canonical]").attr("href"))
                    .isEqualTo("https://autosilent.bg/shumoizolaciya/kit");
        }
    }
}
