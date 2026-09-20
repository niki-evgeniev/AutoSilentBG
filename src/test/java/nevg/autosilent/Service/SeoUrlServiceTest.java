package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.SeoPageMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SeoUrlServiceTest {

    private final SeoUrlService service = new SeoUrlService("https://autosilent.bg/");

    @Test
    void englishCatalogCanonicalKeepsOnlyLanguageAndPagination() {
        MockHttpServletRequest request = request("/shumoizolaciya");
        request.addParameter("lang", "en");
        request.addParameter("page", "1");
        request.addParameter("search", "Vibrofiltr");
        request.addParameter("brand", "Test brand");
        request.addParameter("order", "priceAsc");

        SeoPageMetadata metadata = service.metadata(request, Locale.ENGLISH);

        assertThat(metadata.canonicalUrl())
                .isEqualTo("https://autosilent.bg/shumoizolaciya?lang=en&page=1");
        assertThat(metadata.bulgarianUrl())
                .isEqualTo("https://autosilent.bg/shumoizolaciya?page=1");
        assertThat(metadata.englishUrl()).isEqualTo(metadata.canonicalUrl());
    }

    @Test
    void categoryHasReciprocalLocalizedUrls() {
        MockHttpServletRequest request = request(
                "/shumoizolaciya/category/4/vibroizolatsiya");

        SeoPageMetadata bulgarian = service.metadata(request, Locale.forLanguageTag("bg"));
        SeoPageMetadata english = service.metadata(request, Locale.ENGLISH);

        assertThat(bulgarian.canonicalUrl()).isEqualTo(bulgarian.bulgarianUrl());
        assertThat(english.canonicalUrl()).isEqualTo(english.englishUrl());
        assertThat(english.englishUrl()).endsWith("?lang=en");
    }

    @Test
    void englishProductUiCanonicalizesToBulgarianProductWithoutEnglishAlternate() {
        MockHttpServletRequest request = request("/shumoizolaciya/vibrofiltr-2-0");
        request.addParameter("lang", "en");

        SeoPageMetadata metadata = service.metadata(request, Locale.ENGLISH);

        assertThat(metadata.canonicalUrl())
                .isEqualTo("https://autosilent.bg/shumoizolaciya/vibrofiltr-2-0");
        assertThat(metadata.englishUrl()).isNull();
        assertThat(metadata.contentLanguage()).isEqualTo("bg-BG");
        assertThat(metadata.openGraphLocale()).isEqualTo("bg_BG");
    }

    @Test
    void productSeoCanBeActivatedWhenARealEnglishDescriptionExists() {
        SeoPageMetadata metadata = service.productMetadata(
                "/shumoizolaciya/future-translated-product", Locale.ENGLISH, true);

        assertThat(metadata.canonicalUrl())
                .isEqualTo("https://autosilent.bg/shumoizolaciya/future-translated-product?lang=en");
        assertThat(metadata.englishUrl()).isEqualTo(metadata.canonicalUrl());
        assertThat(metadata.contentLanguage()).isEqualTo("en");
        assertThat(metadata.openGraphLocale()).isEqualTo("en_US");
    }

    @Test
    void languageSwitcherPreservesPageAndFiltersWithoutDuplicatingLanguage() {
        MockHttpServletRequest request = request("/shumoizolaciya");
        request.addParameter("lang", "bg", "en");
        request.addParameter("page", "2");
        request.addParameter("search", "door panel");

        assertThat(service.languageUrl(request, "en"))
                .isEqualTo("/shumoizolaciya?lang=en&page=2&search=door%20panel");
        assertThat(service.languageUrl(request, "bg"))
                .isEqualTo("/shumoizolaciya?page=2&search=door%20panel");
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }
}
