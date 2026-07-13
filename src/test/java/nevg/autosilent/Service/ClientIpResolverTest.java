package nevg.autosilent.Service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void ignoresSpoofableForwardedHeadersByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.20");
        request.addHeader("CF-Connecting-IP", "203.0.113.10");

        assertThat(new ClientIpResolver(false).resolve(request)).isEqualTo("192.0.2.20");
    }

    @Test
    void usesCloudflareHeaderWhenProxyIsTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.20");
        request.addHeader("CF-Connecting-IP", "203.0.113.10");

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void rejectsNonIpForwardedValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.20");
        request.addHeader("CF-Connecting-IP", "attacker.example");

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("192.0.2.20");
    }
}
