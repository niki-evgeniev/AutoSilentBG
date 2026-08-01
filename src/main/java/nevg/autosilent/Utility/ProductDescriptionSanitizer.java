package nevg.autosilent.Utility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public final class ProductDescriptionSanitizer {

    private static final Safelist ALLOWED_HTML = new Safelist()
            .addTags(
                    "div", "p", "br", "hr",
                    "strong", "b", "em", "i", "u", "s",
                    "h2", "h3", "h4", "h5", "h6",
                    "ul", "ol", "li", "blockquote", "pre", "code",
                    "table", "thead", "tbody", "tfoot", "tr", "th", "td",
                    "a"
            )
            .addAttributes("a", "href", "title")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer");

    private static final Document.OutputSettings OUTPUT_SETTINGS =
            new Document.OutputSettings().prettyPrint(false);

    private ProductDescriptionSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html.trim(), "", ALLOWED_HTML, OUTPUT_SETTINGS).trim();
    }

    public static String toPlainText(String html) {
        String sanitized = sanitize(html);
        return sanitized.isBlank() ? "" : Jsoup.parseBodyFragment(sanitized).text();
    }
}
