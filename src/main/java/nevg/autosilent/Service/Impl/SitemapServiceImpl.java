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
    private static final List<String> STATIC_PUBLIC_PATHS = List.of("/", "/products", "/contact");

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
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("urlset");
            xml.writeDefaultNamespace(SITEMAP_NAMESPACE);

            for (String path : STATIC_PUBLIC_PATHS) {
                writeUrl(xml, siteUrl + path, null);
            }
            for (SitemapCategoryDto category : categoryRepository.findAllWithActiveProductsForSitemap()) {
                String categoryPath = "/products/category/" + category.id() + "/"
                        + ProductSlugGenerator.toSlug(category.name());
                writeUrl(xml, siteUrl + categoryPath, category.lastModified());
            }
            for (SitemapProductDto product : productRepository.findAllActiveForSitemap()) {
                writeUrl(xml, siteUrl + "/products/" + product.url(), product.lastModified());
            }

            xml.writeEndElement();
            xml.writeEndDocument();
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
        xml.writeStartElement("url");
        xml.writeStartElement("loc");
        xml.writeCharacters(location);
        xml.writeEndElement();
        if (lastModified != null) {
            xml.writeStartElement("lastmod");
            xml.writeCharacters(lastModified.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            xml.writeEndElement();
        }
        xml.writeEndElement();
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
