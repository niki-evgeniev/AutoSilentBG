package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.SitemapCategoryDto;
import nevg.autosilent.Models.Dto.SitemapProductDto;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Service.SitemapService;
import nevg.autosilent.Utility.ProductSlugGenerator;
import nevg.autosilent.Utility.CatalogCategoryUrlPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SitemapServiceImpl implements SitemapService {

    private static final String SITEMAP_NAMESPACE = "http://www.sitemaps.org/schemas/sitemap/0.9";
    private static final String SITE_URL = "https://autosilent.bg";
    private static final List<String> LOCALIZED_STATIC_PUBLIC_PATHS = List.of("/", "/contact");
    private static final List<String> BULGARIAN_ONLY_STATIC_PUBLIC_PATHS = List.of("/shumoizolaciya");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    public SitemapServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String generateSitemap() {
        try {
            StringWriter output = new StringWriter();
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            xml.setDefaultNamespace(SITEMAP_NAMESPACE);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeCharacters("\n");
            xml.writeStartElement("", "urlset", SITEMAP_NAMESPACE);
            xml.writeDefaultNamespace(SITEMAP_NAMESPACE);
            xml.writeCharacters("\n");
            Set<String> includedUrls = new HashSet<>();

            for (String path : LOCALIZED_STATIC_PUBLIC_PATHS) {
                writeUrlOnce(xml, SITE_URL + path, null, includedUrls);
                writeUrlOnce(xml, SITE_URL + path + "?lang=en", null, includedUrls);
            }
            for (String path : BULGARIAN_ONLY_STATIC_PUBLIC_PATHS) {
                writeUrlOnce(xml, SITE_URL + path, null, includedUrls);
            }
            writeUrlOnce(xml, SITE_URL + "/shumoizolaciya/kit", null, includedUrls);
            for (SitemapCategoryDto category : categoryRepository.findAllWithActiveProductsForSitemap()) {
                if (CatalogCategoryUrlPolicy.isRootCatalogCategory(category.id())) {
                    continue;
                }
                String categoryPath = "/shumoizolaciya/category/" + category.id() + "/"
                        + ProductSlugGenerator.toSlug(category.name());
                writeUrlOnce(xml, SITE_URL + categoryPath, category.lastModified(), includedUrls);
            }
            for (SitemapProductDto product : productRepository.findAllActiveForSitemap()) {
                writeUrlOnce(xml, SITE_URL + "/shumoizolaciya/" + product.url(),
                        product.lastModified(), includedUrls);
            }
            for (SitemapProductDto kit : productRepository.findAllActiveKitsForSitemap()) {
                writeUrlOnce(xml, SITE_URL + "/shumoizolaciya/kit/" + kit.url(),
                        kit.lastModified(), includedUrls);
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

    private void writeUrlOnce(XMLStreamWriter xml, String location, LocalDateTime lastModified,
                              Set<String> includedUrls) throws XMLStreamException {
        if (includedUrls.add(location)) {
            writeUrl(xml, location, lastModified);
        }
    }

    void writeUrl(XMLStreamWriter xml, String location, LocalDateTime lastModified)
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

    private void writeTextElement(XMLStreamWriter xml, String name, String value)
            throws XMLStreamException {
        xml.writeCharacters("    ");
        xml.writeStartElement("", name, SITEMAP_NAMESPACE);
        xml.writeCharacters(value);
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

}
