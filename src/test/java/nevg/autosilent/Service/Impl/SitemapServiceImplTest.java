package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.SitemapCategoryDto;
import nevg.autosilent.Models.Dto.SitemapProductDto;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void sitemapContainsPublicPagesAndActiveProductsWithReliableLastModifiedDate() throws Exception {
        when(productRepository.findAllActiveForSitemap()).thenReturn(List.of(
                new SitemapProductDto("vibrofiltr-2-0", LocalDateTime.of(2026, 7, 21, 14, 30)),
                new SitemapProductDto("special-and-safe", null)
        ));
        when(categoryRepository.findAllWithActiveProductsForSitemap()).thenReturn(List.of(
                new SitemapCategoryDto(1L, "Звукоизолация",
                        LocalDateTime.of(2026, 7, 19, 10, 0)),
                new SitemapCategoryDto(3L, "Звукоизолация",
                        LocalDateTime.of(2026, 7, 20, 10, 0))
        ));
        SitemapServiceImpl service = new SitemapServiceImpl(
                productRepository, categoryRepository, "https://shop.example/");

        String sitemap = service.generateSitemap();
        Document document = parse(sitemap);
        Element root = document.getDocumentElement();
        NodeList locations = document.getElementsByTagNameNS(
                "http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
        NodeList lastModified = document.getElementsByTagNameNS(
                "http://www.sitemaps.org/schemas/sitemap/0.9", "lastmod");
        NodeList alternates = document.getElementsByTagNameNS(
                "http://www.w3.org/1999/xhtml", "link");

        assertThat(sitemap).startsWith("""
                <?xml version="1.0" encoding="UTF-8"?>
                <?xml-stylesheet type="text/xsl" href="/sitemap.xsl"?>

                """);
        assertThat(root.getLocalName()).isEqualTo("urlset");
        assertThat(root.getNamespaceURI())
                .isEqualTo("http://www.sitemaps.org/schemas/sitemap/0.9");
        assertThat(root.lookupNamespaceURI("xhtml"))
                .isEqualTo("http://www.w3.org/1999/xhtml");
        assertThat(textValues(locations)).containsExactly(
                "https://shop.example/",
                "https://shop.example/?lang=en",
                "https://shop.example/contact",
                "https://shop.example/contact?lang=en",
                "https://shop.example/shumoizolaciya",
                "https://shop.example/shumoizolaciya/category/3/zvukoizolatsiya",
                "https://shop.example/shumoizolaciya/vibrofiltr-2-0",
                "https://shop.example/shumoizolaciya/special-and-safe"
        );
        assertThat(textValues(lastModified)).containsExactly("2026-07-20", "2026-07-21");
        assertThat(lastModified.item(0).getParentNode().getLocalName()).isEqualTo("url");
        assertThat(alternates.getLength()).isEqualTo(12);
        assertThat(alternates.item(0).getNamespaceURI())
                .isEqualTo("http://www.w3.org/1999/xhtml");
        assertThat(alternates.item(0).getAttributes().getNamedItem("hreflang").getNodeValue())
                .isEqualTo("bg");
        assertThat(textValues(locations)).noneMatch(url ->
                url.contains("/category/1/zvukoizolatsiya")
                        || url.contains("/shumoizolaciya?lang=en")
                        || url.contains("/category/3/zvukoizolatsiya?lang=en")
                        || url.contains("/vibrofiltr-2-0?lang=en")
                        || url.contains("/special-and-safe?lang=en"));
        assertThat(document.getElementsByTagName("priority").getLength()).isZero();
        assertThat(document.getElementsByTagName("changefreq").getLength()).isZero();
    }

    @Test
    void robotsTxtUsesProductionSitemapAndConfiguredCrawlerRules() {
        SitemapServiceImpl service = new SitemapServiceImpl(
                productRepository, categoryRepository, "https://shop.example");

        assertThat(service.generateRobotsTxt())
                .isEqualTo("""
                        User-agent: *
                        Disallow: /admin/
                        Disallow: /login
                        Disallow: /register
                        Disallow: /cart
                        Disallow: /checkout
                        Disallow: /profile/
                        Disallow: /orders/
                        Disallow: /api/
                        Disallow: /search

                        Sitemap: https://autosilent.bg/sitemap.xml
                        """);
    }

    @Test
    void invalidSiteUrlIsRejectedAtStartup() {
        assertThatThrownBy(() -> new SitemapServiceImpl(
                productRepository, categoryRepository, "shop.example"))
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
