package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TopBarTemplateTest {

    private static final Path TEMPLATE = Path.of(
            "src/main/resources/templates/fragments/top-bar.html");

    @Test
    void settingsDropdownLinksToPromoCodesForAdmins() throws IOException {
        String html = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

        assertThat(html)
                .contains("top-settings-dropdown")
                .contains("sec:authorize=\"hasRole('ADMIN')\"")
                .contains("th:href=\"@{/admin/settings/promo-codes}\"")
                .contains("th:text=\"#{admin.promoCodes.heading}\"");
    }
}
