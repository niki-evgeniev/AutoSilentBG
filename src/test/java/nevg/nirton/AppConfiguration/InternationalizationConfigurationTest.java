package nevg.nirton.AppConfiguration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class InternationalizationConfigurationTest {

    private final LocaleResolver localeResolver =
            new InternationalizationConfiguration().localeResolver();

    @Test
    void usesBulgarianBrowserLocaleWhenCookieIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(Locale.forLanguageTag("bg-BG"));

        assertThat(localeResolver.resolveLocale(request).getLanguage()).isEqualTo("bg");
    }

    @Test
    void storesAndRestoresSelectedLocaleFromCookie() {
        MockHttpServletRequest selectionRequest = new MockHttpServletRequest();
        MockHttpServletResponse selectionResponse = new MockHttpServletResponse();
        localeResolver.setLocale(selectionRequest, selectionResponse, Locale.ENGLISH);

        Cookie localeCookie = selectionResponse.getCookie("NIRTON_LOCALE");
        assertThat(localeCookie).isNotNull();
        assertThat(localeCookie.getMaxAge()).isEqualTo(365 * 24 * 60 * 60);
        assertThat(localeCookie.getPath()).isEqualTo("/");

        MockHttpServletRequest laterRequest = new MockHttpServletRequest();
        laterRequest.addPreferredLocale(Locale.forLanguageTag("bg-BG"));
        laterRequest.setCookies(localeCookie);

        assertThat(localeResolver.resolveLocale(laterRequest)).isEqualTo(Locale.ENGLISH);
    }
}
