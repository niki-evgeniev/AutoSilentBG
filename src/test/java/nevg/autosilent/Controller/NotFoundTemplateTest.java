package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NotFoundTemplateTest {

    @Test
    void notFoundPageIsUserFriendlyAndNotIndexable() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/error/404.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("<meta name=\"robots\" content=\"noindex\">")
                .contains("class=\"error-code\">404")
                .contains("th:href=\"@{/}\"")
                .contains("th:href=\"@{/shumoizolaciya}\"");
    }
}
