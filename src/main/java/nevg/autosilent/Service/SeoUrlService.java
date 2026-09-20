package nevg.autosilent.Service;

import jakarta.servlet.http.HttpServletRequest;
import nevg.autosilent.Models.Dto.SeoPageMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SeoUrlService {

    private static final Set<String> FULLY_LOCALIZED_PATHS = Set.of("/", "/contact");

    private final String siteUrl;

    public SeoUrlService(@Value("${AutoSilent.site-url:https://autosilent.bg}") String siteUrl) {
        String normalized = siteUrl == null ? "" : siteUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        this.siteUrl = normalized;
    }

    public SeoPageMetadata metadata(HttpServletRequest request, Locale locale) {
        String path = requestPath(request);
        int page = catalogPage(path, request.getParameter("page"));
        if (isCatalogPath(path)) {
            return catalogMetadata(path, page, locale, false);
        }
        if (isProductPath(path)) {
            return productMetadata(path, locale, false);
        }

        boolean fullyLocalized = FULLY_LOCALIZED_PATHS.contains(path);
        String bulgarianUrl = localizedUrl(path, false, page);
        String englishUrl = fullyLocalized ? localizedUrl(path, true, page) : null;
        boolean englishContent = fullyLocalized && "en".equals(locale.getLanguage());
        return new SeoPageMetadata(
                englishContent ? englishUrl : bulgarianUrl,
                bulgarianUrl,
                englishUrl,
                englishContent ? "en" : "bg-BG",
                englishContent ? "en_US" : "bg_BG",
                englishContent
        );
    }

    public SeoPageMetadata catalogMetadata(String path, int page, Locale locale,
                                           boolean hasFullyTranslatedCatalogContent) {
        String bulgarianUrl = localizedUrl(path, false, page);
        String englishUrl = hasFullyTranslatedCatalogContent
                ? localizedUrl(path, true, page) : null;
        boolean englishContent = hasFullyTranslatedCatalogContent
                && "en".equals(locale.getLanguage());
        return new SeoPageMetadata(
                englishContent ? englishUrl : bulgarianUrl,
                bulgarianUrl,
                englishUrl,
                englishContent ? "en" : "bg-BG",
                englishContent ? "en_US" : "bg_BG",
                englishContent
        );
    }

    public SeoPageMetadata productMetadata(String path, Locale locale,
                                           boolean hasEnglishDescription) {
        String bulgarianUrl = localizedUrl(path, false, 0);
        String englishUrl = hasEnglishDescription ? localizedUrl(path, true, 0) : null;
        boolean englishContent = hasEnglishDescription && "en".equals(locale.getLanguage());
        return new SeoPageMetadata(
                englishContent ? englishUrl : bulgarianUrl,
                bulgarianUrl,
                englishUrl,
                englishContent ? "en" : "bg-BG",
                englishContent ? "en_US" : "bg_BG",
                englishContent
        );
    }

    public String languageUrl(HttpServletRequest request, String language) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(requestPath(request));
        builder.queryParam("lang", language);
        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            if ("lang".equals(parameter.getKey())) {
                continue;
            }
            for (String value : parameter.getValue()) {
                builder.queryParam(parameter.getKey(), value);
            }
        }
        return builder.build().encode().toUriString();
    }

    private boolean isCatalogPath(String path) {
        return path.equals("/shumoizolaciya")
                || path.startsWith("/shumoizolaciya/category/");
    }

    private boolean isProductPath(String path) {
        return path.startsWith("/shumoizolaciya/")
                && !path.startsWith("/shumoizolaciya/category/");
    }

    private int catalogPage(String path, String rawPage) {
        if (!(path.equals("/shumoizolaciya") || path.startsWith("/shumoizolaciya/category/"))) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(rawPage));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String localizedUrl(String path, boolean english, int page) {
        StringBuilder url = new StringBuilder(siteUrl).append(path);
        if (english) {
            url.append("?lang=en");
        }
        if (page > 0) {
            url.append(english ? '&' : '?').append("page=").append(page);
        }
        return url.toString();
    }

    private String requestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path == null || path.isBlank() ? "/" : path;
    }
}
