package nevg.autosilent.AppConfiguration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

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
    void usesBulgarianDefaultWhenBrowserLocaleIsNotBulgarianAndCookieIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(Locale.ENGLISH);

        assertThat(localeResolver.resolveLocale(request).getLanguage()).isEqualTo("bg");
    }

    @Test
    void storesAndRestoresSelectedLocaleFromCookie() {
        MockHttpServletRequest selectionRequest = new MockHttpServletRequest();
        MockHttpServletResponse selectionResponse = new MockHttpServletResponse();
        localeResolver.setLocale(selectionRequest, selectionResponse, Locale.ENGLISH);

        Cookie localeCookie = selectionResponse.getCookie("AutoSilent_LOCALE");
        assertThat(localeCookie).isNotNull();
        assertThat(localeCookie.getMaxAge()).isEqualTo(365 * 24 * 60 * 60);
        assertThat(localeCookie.getPath()).isEqualTo("/");

        MockHttpServletRequest laterRequest = new MockHttpServletRequest();
        laterRequest.addPreferredLocale(Locale.forLanguageTag("bg-BG"));
        laterRequest.setCookies(localeCookie);

        assertThat(localeResolver.resolveLocale(laterRequest)).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void bgLanguageSwitchReplacesEnglishCookieForFollowingRequest() throws Exception {
        MockHttpServletRequest englishRequest = new MockHttpServletRequest();
        MockHttpServletResponse englishResponse = new MockHttpServletResponse();
        localeResolver.setLocale(englishRequest, englishResponse, Locale.ENGLISH);
        Cookie englishCookie = englishResponse.getCookie("AutoSilent_LOCALE");

        MockHttpServletRequest switchRequest = new MockHttpServletRequest("GET", "/shumoizolaciya");
        switchRequest.setCookies(englishCookie);
        switchRequest.addParameter("lang", "bg");
        switchRequest.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, localeResolver);
        MockHttpServletResponse switchResponse = new MockHttpServletResponse();
        LocaleChangeInterceptor interceptor =
                new InternationalizationConfiguration().localeChangeInterceptor();

        interceptor.preHandle(switchRequest, switchResponse, new Object());

        Cookie bulgarianCookie = switchResponse.getCookie("AutoSilent_LOCALE");
        assertThat(bulgarianCookie).isNotNull();
        MockHttpServletRequest followingRequest = new MockHttpServletRequest();
        followingRequest.setCookies(bulgarianCookie);
        assertThat(localeResolver.resolveLocale(followingRequest).getLanguage()).isEqualTo("bg");
    }
}
