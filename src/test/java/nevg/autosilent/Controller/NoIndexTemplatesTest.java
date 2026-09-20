package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NoIndexTemplatesTest {

    @Test
    void publicSeoPagesAreIndexable() throws Exception {
        for (String template : new String[]{"index.html", "products.html", "product-details.html"}) {
            String html = Files.readString(
                    Path.of("src/main/resources/templates", template), StandardCharsets.UTF_8);
            assertThat(html)
                    .as("robots directive in %s", template)
                    .contains("<meta name=\"robots\" content=\"index, follow,")
                    .doesNotContain("<meta name=\"robots\" content=\"noindex\">");
        }
    }

    @Test
    void accountAndTransactionalPagesAreNoindex() throws Exception {
        for (String template : new String[]{
                "login.html", "register.html", "forgot-password.html", "profile.html",
                "account-dashboard.html", "user-addresses.html", "user-orders.html",
                "user-order-details.html", "favorites.html", "cart.html", "checkout.html"
        }) {
            String html = Files.readString(
                    Path.of("src/main/resources/templates", template), StandardCharsets.UTF_8);
            assertThat(html)
                    .as("robots directive in %s", template)
                    .contains("<meta name=\"robots\" content=\"noindex\">");
        }
    }

    @Test
    void everyRenderedTemplateUsesTheActiveLocaleForHtmlLang() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/resources/templates"))) {
            for (Path template : paths.filter(path -> path.toString().endsWith(".html")).toList()) {
                assertThat(Files.readString(template, StandardCharsets.UTF_8))
                        .as("html language in %s", template)
                        .contains("th:lang=\"${#locale.language}\"");
            }
        }
    }
}
