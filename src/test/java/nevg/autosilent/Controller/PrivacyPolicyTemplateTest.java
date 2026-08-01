package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PrivacyPolicyTemplateTest {

    @Test
    void footerLinksToPrivacyPolicyInsteadOfSizesAndCompatibility() throws Exception {
        String footer = read("fragments/footer.html");

        assertThat(footer)
                .contains("th:href=\"@{/privacy-policy}\"")
                .contains("th:text=\"#{footer.privacy}\"")
                .doesNotContain("th:text=\"#{footer.sizes}\"");
    }

    @Test
    void privacyPolicyContainsAllNumberedSectionsAndContactDetails() throws Exception {
        String policy = read("privacy-policy.html");

        for (int section = 1; section <= 18; section++) {
            assertThat(policy).contains("<h2>" + section + ".");
        }
        assertThat(policy)
                .contains("Последна актуализация: 2 август 2026 г.")
                .contains("autosilentbg@gmail.com")
                .contains("+359 895 02 03 04")
                .contains("[ПЪЛНО НАИМЕНОВАНИЕ НА ФИРМАТА]");
    }

    private String read(String name) throws Exception {
        return Files.readString(Path.of("src/main/resources/templates", name), StandardCharsets.UTF_8);
    }
}
