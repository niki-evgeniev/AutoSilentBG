package nevg.autosilent.AppConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalHostConfigurationTest {

    @Test
    void redirectsHttpAndWwwWhilePreservingPathAndQuery() throws Exception {
        var filter = new CanonicalHostConfiguration().canonicalHostFilter().getFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/shumoizolaciya");
        request.setServerName("www.autosilent.bg");
        request.addHeader("X-Forwarded-Proto", "http");
        request.setQueryString("page=2&search=a%26b");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedChain = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> reachedChain.set(true));

        assertThat(response.getStatus()).isEqualTo(308);
        assertThat(response.getHeader("Location"))
                .isEqualTo("https://autosilent.bg/shumoizolaciya?page=2&search=a%26b");
        assertThat(reachedChain).isFalse();
    }

    @Test
    void acceptsForwardedHttpsFromProxyWithoutRedirectLoop() throws Exception {
        var filter = new CanonicalHostConfiguration().canonicalHostFilter().getFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/shumoizolaciya");
        request.setServerName("autosilent.bg");
        request.setScheme("http");
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedChain = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> reachedChain.set(true));

        assertThat(reachedChain).isTrue();
        assertThat(response.getHeader("Location")).isNull();
    }

    @ParameterizedTest
    @CsvSource({"autosilent.bg, http", "www.autosilent.bg, https"})
    void redirectsEachNoncanonicalOriginIndependently(String host, String scheme) throws Exception {
        var filter = new CanonicalHostConfiguration().canonicalHostFilter().getFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/contact");
        request.setServerName(host);
        request.addHeader("X-Forwarded-Proto", scheme);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(308);
        assertThat(response.getHeader("Location")).isEqualTo("https://autosilent.bg/contact");
    }

    @Test
    void runsBeforeTrailingSlashAndSecurityFilters() {
        assertThat(new CanonicalHostConfiguration().canonicalHostFilter().getOrder()).isLessThan(-101);
    }
}
