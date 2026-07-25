package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoIndexTemplatesTest {

    private static final String ROBOTS_NOINDEX =
            "<meta name=\"robots\" content=\"noindex\">";

    @Test
    void everyRenderablePageIsTemporarilyExcludedFromIndexing() throws Exception {
        Path templatesDirectory = Path.of("src/main/resources/templates");
        List<Path> pages;
        try (var paths = Files.walk(templatesDirectory)) {
            pages = paths
                    .filter(path -> path.toString().endsWith(".html"))
                    .filter(path -> !path.toString().contains(
                            java.io.File.separator + "fragments" + java.io.File.separator))
                    .toList();
        }

        assertThat(pages).isNotEmpty();
        for (Path page : pages) {
            String html = Files.readString(page, StandardCharsets.UTF_8);
            if (html.contains("<head")) {
                assertThat(html)
                        .as("robots directive in %s", page)
                        .containsOnlyOnce(ROBOTS_NOINDEX);
            }
        }
    }
}
