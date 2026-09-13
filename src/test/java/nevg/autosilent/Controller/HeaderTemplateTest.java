package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderTemplateTest {

    private static final Path TEMPLATE = Path.of(
            "src/main/resources/templates/fragments/header.html");

    @Test
    void headerExposesMainNavigationLinks() throws IOException {
        String html = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

        assertThat(html)
                .contains("th:fragment=\"header\"")
                .contains("data-nav=\"home\"")
                .contains("th:href=\"@{/}\"")
                .contains("data-nav=\"products\"")
                .contains("th:href=\"@{/shumoizolaciya}\"")
                .contains("data-nav=\"contacts\"")
                .contains("th:href=\"@{/contact}\"");
    }

    @Test
    void headerShowsAuthenticationAwareActionsAndFavoriteBadge() throws IOException {
        String html = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

        assertThat(html)
                .contains("sec:authorize=\"isAuthenticated()\"")
                .contains("th:href=\"@{/user/account}\"")
                .contains("sec:authorize=\"isAnonymous()\"")
                .contains("th:href=\"@{/user/sign_in}\"")
                .contains("th:href=\"@{/favorites}\"")
                .contains("th:if=\"${favoriteProductIds != null and !favoriteProductIds.isEmpty()}\"")
                .contains("th:text=\"${favoriteProductIds.size()}\"");
    }

    @Test
    void headerContainsSearchLanguageAndCartControls() throws IOException {
        String html = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

        assertThat(html)
                .contains("class=\"language-switch\"")
                .contains("href=\"?lang=bg\"")
                .contains("href=\"?lang=en\"")
                .contains("th:action=\"@{/shumoizolaciya}\"")
                .contains("method=\"get\"")
                .contains("name=\"search\"")
                .contains("maxlength=\"150\"")
                .contains("th:href=\"@{/cart}\"")
                .contains("class=\"cart-count\"")
                .contains("class=\"cart-total\"");
    }
}
