package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.SitemapProductDto;
import nevg.autosilent.Repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SitemapServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Test
    void sitemapContainsPublicPagesAndActiveProductsWithReliableLastModifiedDate() throws Exception {
        when(productRepository.findAllActiveForSitemap()).thenReturn(List.of(
                new SitemapProductDto("vibrofiltr-2-0", LocalDateTime.of(2026, 7, 21, 14, 30)),
                new SitemapProductDto("special-and-safe", null)
        ));
        SitemapServiceImpl service = new SitemapServiceImpl(productRepository, "https://shop.example/");

        Document document = parse(service.generateSitemap());
        NodeList locations = document.getElementsByTagNameNS(
                "http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
        NodeList lastModified = document.getElementsByTagNameNS(
                "http://www.sitemaps.org/schemas/sitemap/0.9", "lastmod");

        assertThat(textValues(locations)).containsExactly(
                "https://shop.example/",
                "https://shop.example/products",
                "https://shop.example/contact",
                "https://shop.example/products/vibrofiltr-2-0",
                "https://shop.example/products/special-and-safe"
        );
        assertThat(textValues(lastModified)).containsExactly("2026-07-21");
        assertThat(document.getElementsByTagName("priority").getLength()).isZero();
        assertThat(document.getElementsByTagName("changefreq").getLength()).isZero();
    }

    @Test
    void robotsTxtReferencesSitemapAndBlocksPrivateAreas() {
        SitemapServiceImpl service = new SitemapServiceImpl(productRepository, "https://shop.example");

        assertThat(service.generateRobotsTxt())
                .contains("User-agent: *", "Allow: /")
                .contains("Disallow: /admin/", "Disallow: /user/", "Disallow: /orders/")
                .contains("Sitemap: https://shop.example/sitemap.xml");
    }

    @Test
    void invalidSiteUrlIsRejectedAtStartup() {
        assertThatThrownBy(() -> new SitemapServiceImpl(productRepository, "shop.example"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute HTTP(S) URL");
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private List<String> textValues(NodeList nodes) {
        return java.util.stream.IntStream.range(0, nodes.getLength())
                .mapToObj(index -> nodes.item(index).getTextContent())
                .toList();
    }
}
