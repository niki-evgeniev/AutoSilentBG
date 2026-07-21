package nevg.autosilent.Controller;

import nevg.autosilent.Service.SitemapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SitemapControllerTest {

    @Mock
    private SitemapService sitemapService;

    private SitemapController controller;

    @BeforeEach
    void setUp() {
        controller = new SitemapController(sitemapService);
    }

    @Test
    void sitemapReturnsXmlWithPublicCacheHeaders() {
        when(sitemapService.generateSitemap()).thenReturn("<urlset/>");

        ResponseEntity<String> response = controller.sitemap();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/xml;charset=UTF-8");
        assertThat(response.getHeaders().getCacheControl()).contains("public", "max-age=3600");
        assertThat(response.getBody()).isEqualTo("<urlset/>");
    }

    @Test
    void robotsReturnsPlainText() {
        when(sitemapService.generateRobotsTxt()).thenReturn("User-agent: *");

        ResponseEntity<String> response = controller.robotsTxt();

        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("text/plain;charset=UTF-8");
        assertThat(response.getBody()).isEqualTo("User-agent: *");
    }
}
