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
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
                new SitemapProductDto("special-and-safe", null),
                new SitemapProductDto("vibrofiltr-2-0", LocalDateTime.of(2026, 7, 21, 14, 30))
        ));
        when(productRepository.findAllActiveKitsForSitemap()).thenReturn(List.of(
                new SitemapProductDto("front-door-kit", LocalDateTime.of(2026, 7, 22, 11, 0))));
        when(categoryRepository.findAllWithActiveProductsForSitemap()).thenReturn(List.of(
                new SitemapCategoryDto(1L, "Звукоизолация",
                        LocalDateTime.of(2026, 7, 19, 10, 0)),
                new SitemapCategoryDto(3L, "Звукоизолация",
                        LocalDateTime.of(2026, 7, 20, 10, 0))
        ));
        SitemapServiceImpl service = new SitemapServiceImpl(productRepository, categoryRepository);

        String sitemap = service.generateSitemap();
        Document document = parse(sitemap);
        Element root = document.getDocumentElement();
        NodeList locations = document.getElementsByTagNameNS(
                "http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
        NodeList lastModified = document.getElementsByTagNameNS(
                "http://www.sitemaps.org/schemas/sitemap/0.9", "lastmod");

        assertThat(sitemap).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset")
                .doesNotContain("xml-stylesheet");
        assertThat(root.getLocalName()).isEqualTo("urlset");
        assertThat(root.getNamespaceURI())
                .isEqualTo("http://www.sitemaps.org/schemas/sitemap/0.9");
        assertThat(root.lookupNamespaceURI("xhtml")).isNull();
        assertThat(textValues(locations)).containsExactly(
                "https://autosilent.bg/",
                "https://autosilent.bg/?lang=en",
                "https://autosilent.bg/contact",
                "https://autosilent.bg/contact?lang=en",
                "https://autosilent.bg/shumoizolaciya",
                "https://autosilent.bg/shumoizolaciya/kit",
                "https://autosilent.bg/shumoizolaciya/category/3/zvukoizolatsiya",
                "https://autosilent.bg/shumoizolaciya/vibrofiltr-2-0",
                "https://autosilent.bg/shumoizolaciya/special-and-safe",
                "https://autosilent.bg/shumoizolaciya/kit/front-door-kit"
        );
        assertThat(textValues(lastModified)).containsExactly("2026-07-20", "2026-07-21", "2026-07-22");
        assertThat(lastModified.item(0).getParentNode().getLocalName()).isEqualTo("url");
        assertThat(textValues(locations)).noneMatch(url ->
                url.contains("/category/1/zvukoizolatsiya")
                        || url.contains("/shumoizolaciya?lang=en")
                        || url.contains("/category/3/zvukoizolatsiya?lang=en")
                        || url.contains("/vibrofiltr-2-0?lang=en")
                        || url.contains("/special-and-safe?lang=en"));
        assertThat(textValues(locations)).doesNotHaveDuplicates()
                .allMatch(url -> url.startsWith("https://autosilent.bg/"));
        assertThat(sitemap).doesNotContain("<priority>", "<changefreq>", "http://autosilent.bg",
                "www.autosilent.bg", "xmlns:xhtml", "xhtml:link");
        assertThat(document.getElementsByTagName("priority").getLength()).isZero();
        assertThat(document.getElementsByTagName("changefreq").getLength()).isZero();
    }

    @Test
    void xmlWriterEscapesAmpersandsInQueryParameters() throws Exception {
        SitemapServiceImpl service = new SitemapServiceImpl(productRepository, categoryRepository);
        StringWriter output = new StringWriter();
        XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
        xml.writeStartElement("urlset");
        service.writeUrl(xml, "https://autosilent.bg/contact?lang=en&ref=a&b", null);
        xml.writeEndElement();
        xml.close();

        assertThat(output.toString()).contains("?lang=en&amp;ref=a&amp;b");
        assertThat(parse(output.toString()).getElementsByTagName("loc").item(0).getTextContent())
                .isEqualTo("https://autosilent.bg/contact?lang=en&ref=a&b");
    }

    @Test
    void sitemapListsOnlyExistingCatalogPagesWithoutInventedDates() throws Exception {
        when(productRepository.countByActiveTrue()).thenReturn(19L);
        SitemapServiceImpl service = new SitemapServiceImpl(productRepository, categoryRepository);

        Document document = parse(service.generateSitemap());
        List<String> urls = textValues(document.getElementsByTagNameNS(
                "http://www.sitemaps.org/schemas/sitemap/0.9", "loc"));

        assertThat(urls).contains("https://autosilent.bg/shumoizolaciya?page=1",
                "https://autosilent.bg/shumoizolaciya?page=2")
                .doesNotContain("https://autosilent.bg/shumoizolaciya?page=0",
                        "https://autosilent.bg/shumoizolaciya?page=3")
                .doesNotHaveDuplicates();
        assertThat(document.getElementsByTagNameNS(
                "http://www.sitemaps.org/schemas/sitemap/0.9", "lastmod").getLength()).isZero();
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
