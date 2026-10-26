package nevg.autosilent.AppConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.UrlHandlerFilter;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class TrailingSlashConfigurationTest {

    private final UrlHandlerFilter filter =
            new TrailingSlashConfiguration().trailingSlashFilter().getFilter();

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST"})
    void redirectsToCanonicalPathBeforeReachingDownstreamFilters(String method) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/shumoizolaciya/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedChain = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> reachedChain.set(true));

        assertThat(response.getStatus()).isEqualTo(308);
        assertThat(response.getHeader("Location")).isEqualTo("/shumoizolaciya");
        assertThat(reachedChain).isFalse();
    }

    @Test
    void preservesEncodedQueryParameters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/shumoizolaciya/");
        request.setQueryString("page=2&search=%D1%80%D0%B8%D0%B1%D0%B0&next=a%2Fb");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader("Location")).isEqualTo(
                "/shumoizolaciya?page=2&search=%D1%80%D0%B8%D0%B1%D0%B0&next=a%2Fb");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/shumoizolaciya", "/css/style.css"})
    void letsRootAndPathsWithoutTrailingSlashContinue(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedChain = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> reachedChain.set(true));

        assertThat(reachedChain).isTrue();
        assertThat(response.getHeader("Location")).isNull();
    }

    @Test
    void preservesServletContextPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/shumoizolaciya/");
        request.setContextPath("/app");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader("Location")).isEqualTo("/app/shumoizolaciya");
    }

    @Test
    void runsBeforeDefaultSecurityFilterOrder() {
        assertThat(new TrailingSlashConfiguration().trailingSlashFilter().getOrder())
                .isLessThan(-100);
    }
}
