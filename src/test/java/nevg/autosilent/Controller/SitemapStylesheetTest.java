package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SitemapStylesheetTest {

    private static final String XSL_NAMESPACE = "http://www.w3.org/1999/XSL/Transform";
    private static final Path STYLESHEET = Path.of("src/main/resources/static/sitemap.xsl");

    @Test
    void stylesheetIsValidStandaloneXmlAndUsesSitemapNamespaces() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(STYLESHEET.toFile());

        assertThat(document.getDocumentElement().getLocalName()).isEqualTo("stylesheet");
        assertThat(document.getDocumentElement().getNamespaceURI()).isEqualTo(XSL_NAMESPACE);
        assertThat(document.getDocumentElement().lookupNamespaceURI("s"))
                .isEqualTo("http://www.sitemaps.org/schemas/sitemap/0.9");
        assertThat(document.getDocumentElement().lookupNamespaceURI("xhtml"))
                .isEqualTo("http://www.w3.org/1999/xhtml");
        assertThat(document.getElementsByTagNameNS(XSL_NAMESPACE, "for-each").getLength())
                .isGreaterThanOrEqualTo(2);
        assertThat(document.getElementsByTagName("script").getLength()).isZero();

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transformerFactory.newTemplates(new DOMSource(document));
    }
}
