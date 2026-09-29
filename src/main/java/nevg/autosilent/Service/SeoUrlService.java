package nevg.autosilent.Service;

import jakarta.servlet.http.HttpServletRequest;
import nevg.autosilent.Models.Dto.SeoPageMetadata;

import java.util.Locale;

public interface SeoUrlService {
    SeoPageMetadata metadata(HttpServletRequest request, Locale locale);

    SeoPageMetadata catalogMetadata(String path, int page, Locale locale,
                                    boolean hasFullyTranslatedCatalogContent);

    SeoPageMetadata productMetadata(String path, Locale locale, boolean hasEnglishDescription);

    String languageUrl(HttpServletRequest request, String language);
}
