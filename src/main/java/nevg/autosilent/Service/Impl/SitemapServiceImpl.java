package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.SitemapCategoryDto;
import nevg.autosilent.Models.Dto.SitemapProductDto;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Service.SitemapService;
import nevg.autosilent.Utility.ProductSlugGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class SitemapServiceImpl implements SitemapService {

    private static final String SITEMAP_NAMESPACE = "http://www.sitemaps.org/schemas/sitemap/0.9";
    private static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    private static final List<String> LOCALIZED_STATIC_PUBLIC_PATHS = List.of("/", "/contact");
    private static final List<String> BULGARIAN_ONLY_STATIC_PUBLIC_PATHS = List.of("/shumoizolaciya");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final String siteUrl;

    public SitemapServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              @Value("${AutoSilent.site-url:https://autosilent.bg}") String siteUrl) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.siteUrl = normalizeSiteUrl(siteUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateSitemap() {
        try {
            StringWriter output = new StringWriter();
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            xml.setDefaultNamespace(SITEMAP_NAMESPACE);
            xml.setPrefix("xhtml", XHTML_NAMESPACE);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeCharacters("\n");
            xml.writeProcessingInstruction("xml-stylesheet",
                    "type=\"text/xsl\" href=\"/sitemap.xsl\"");
            xml.writeCharacters("\n\n");
            xml.writeStartElement("", "urlset", SITEMAP_NAMESPACE);
            xml.writeDefaultNamespace(SITEMAP_NAMESPACE);
            xml.writeNamespace("xhtml", XHTML_NAMESPACE);
            xml.writeCharacters("\n");

            for (String path : LOCALIZED_STATIC_PUBLIC_PATHS) {
                writeLocalizedUrls(xml, siteUrl + path, null);
            }
            for (String path : BULGARIAN_ONLY_STATIC_PUBLIC_PATHS) {
                writeUrl(xml, siteUrl + path, null);
            }
            for (SitemapCategoryDto category : categoryRepository.findAllWithActiveProductsForSitemap()) {
                String categoryPath = "/shumoizolaciya/category/" + category.id() + "/"
                        + ProductSlugGenerator.toSlug(category.name());
                writeUrl(xml, siteUrl + categoryPath, category.lastModified());
            }
            for (SitemapProductDto product : productRepository.findAllActiveForSitemap()) {
                writeUrl(xml, siteUrl + "/shumoizolaciya/" + product.url(), product.lastModified());
            }

            xml.writeEndElement();
            xml.writeCharacters("\n");
            xml.writeEndDocument();
            xml.flush();
            xml.close();
            return output.toString();
        } catch (XMLStreamException exception) {
            throw new IllegalStateException("Sitemap XML could not be generated.", exception);
        }
    }

    @Override
    public String generateRobotsTxt() {
        return """
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
                """;
    }

    private void writeUrl(XMLStreamWriter xml, String location, LocalDateTime lastModified)
            throws XMLStreamException {
        xml.writeCharacters("  ");
        xml.writeStartElement("", "url", SITEMAP_NAMESPACE);
        xml.writeCharacters("\n");
        writeTextElement(xml, "loc", location);
        if (lastModified != null) {
            writeTextElement(xml, "lastmod",
                    lastModified.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        xml.writeCharacters("  ");
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    private void writeLocalizedUrls(XMLStreamWriter xml, String bulgarianUrl,
                                    LocalDateTime lastModified) throws XMLStreamException {
        String englishUrl = bulgarianUrl + "?lang=en";
        writeLocalizedUrl(xml, bulgarianUrl, bulgarianUrl, englishUrl, lastModified);
        writeLocalizedUrl(xml, englishUrl, bulgarianUrl, englishUrl, lastModified);
    }

    private void writeLocalizedUrl(XMLStreamWriter xml, String location, String bulgarianUrl,
                                   String englishUrl, LocalDateTime lastModified)
            throws XMLStreamException {
        xml.writeCharacters("  ");
        xml.writeStartElement("", "url", SITEMAP_NAMESPACE);
        xml.writeCharacters("\n");
        writeTextElement(xml, "loc", location);
        writeAlternate(xml, "bg", bulgarianUrl);
        writeAlternate(xml, "en", englishUrl);
        writeAlternate(xml, "x-default", bulgarianUrl);
        if (lastModified != null) {
            writeTextElement(xml, "lastmod",
                    lastModified.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        xml.writeCharacters("  ");
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    private void writeAlternate(XMLStreamWriter xml, String language, String url)
            throws XMLStreamException {
        xml.writeCharacters("    ");
        xml.writeEmptyElement("xhtml", "link", XHTML_NAMESPACE);
        xml.writeAttribute("rel", "alternate");
        xml.writeAttribute("hreflang", language);
        xml.writeAttribute("href", url);
        xml.writeCharacters("\n");
    }

    private void writeTextElement(XMLStreamWriter xml, String name, String value)
            throws XMLStreamException {
        xml.writeCharacters("    ");
        xml.writeStartElement("", name, SITEMAP_NAMESPACE);
        xml.writeCharacters(value);
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    private String normalizeSiteUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("AutoSilent.site-url must be a valid absolute URL.", exception);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "AutoSilent.site-url must be an absolute HTTP(S) URL without query or fragment.");
        }
        return normalized;
    }
}
