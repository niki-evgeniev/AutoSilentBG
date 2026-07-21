package nevg.autosilent.Controller;

import nevg.autosilent.Service.SitemapService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Controller
public class SitemapController {

    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
    private static final MediaType XML_UTF_8 = new MediaType("application",
            "xml", StandardCharsets.UTF_8);

    private final SitemapService sitemapService;

    public SitemapController(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @GetMapping("/sitemap.xml")
    @ResponseBody
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok()
                .contentType(XML_UTF_8)
                .cacheControl(PUBLIC_CACHE)
                .body(sitemapService.generateSitemap());
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<String> robotsTxt() {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .cacheControl(PUBLIC_CACHE)
                .body(sitemapService.generateRobotsTxt());
    }
}
