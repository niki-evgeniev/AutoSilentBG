package nevg.autosilent.Scheduler;

import nevg.autosilent.Service.SitemapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SitemapSchedulerTest {

    @TempDir Path temporaryDirectory;

    @Test
    void writesAndReplacesSitemapFileWithoutLeavingTemporaryFiles() throws Exception {
        SitemapService sitemapService = mock(SitemapService.class);
        Path directory = temporaryDirectory.resolve("sitemap");
        SitemapScheduler scheduler = new SitemapScheduler(sitemapService, directory.toString());
        when(sitemapService.generateSitemap()).thenReturn("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset/>")
                .thenReturn("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset><url/></urlset>");

        scheduler.generateOnStartup();
        assertThat(Files.readString(directory.resolve("sitemap.xml"), StandardCharsets.UTF_8))
                .isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset/>");
        scheduler.createSitemap();
        assertThat(Files.readString(directory.resolve("sitemap.xml"), StandardCharsets.UTF_8))
                .isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset><url/></urlset>");
        try (var files = Files.list(directory)) {
            assertThat(files.map(Path::getFileName).map(Path::toString).toList())
                    .containsExactly("sitemap.xml");
        }
    }

    @Test
    void failedGenerationKeepsPreviousSitemap() throws Exception {
        SitemapService sitemapService = mock(SitemapService.class);
        Path directory = temporaryDirectory.resolve("sitemap");
        Files.createDirectories(directory);
        Path target = directory.resolve("sitemap.xml");
        Files.writeString(target, "previous sitemap", StandardCharsets.UTF_8);
        when(sitemapService.generateSitemap()).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> new SitemapScheduler(sitemapService, directory.toString()).createSitemap())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo("previous sitemap");
        try (var files = Files.list(directory)) {
            assertThat(files.map(Path::getFileName).map(Path::toString).toList())
                    .containsExactly("sitemap.xml");
        }
    }
}
