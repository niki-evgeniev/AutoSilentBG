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
