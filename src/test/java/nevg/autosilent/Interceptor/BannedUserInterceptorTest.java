package nevg.autosilent.Interceptor;

import jakarta.servlet.http.HttpServletResponse;
import nevg.autosilent.Service.BannedUserService;
import nevg.autosilent.Service.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.View;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BannedUserInterceptorTest {

    @Mock
    private ThymeleafViewResolver thymeleafViewResolver;
    @Mock
    private BannedUserService bannedUserService;
    @Mock
    private ClientIpResolver clientIpResolver;
    @Mock
    private View bannedView;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setPreferredLocales(List.of(Locale.ENGLISH));
        response = new MockHttpServletResponse();
    }

    @Test
    void preHandleAllowsRequestWithoutCheckingWhenDisabled() throws Exception {
        BannedUserInterceptor interceptor = interceptor(false);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verifyNoInteractions(clientIpResolver, bannedUserService, thymeleafViewResolver);
    }

    @Test
    void preHandleAllowsRequestWhenAddressIsNotBanned() throws Exception {
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");
        when(bannedUserService.recordVisitAndCheckIfBanned("203.0.113.10", null)).thenReturn(false);

        boolean result = interceptor(true).preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(thymeleafViewResolver, never()).resolveViewName(any(), any());
    }

    @Test
    void preHandlePassesAuthenticatedPrincipalNameToBanService() throws Exception {
        request.setUserPrincipal((Principal) () -> "user@example.com");
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");
        when(bannedUserService.recordVisitAndCheckIfBanned("203.0.113.10", "user@example.com"))
                .thenReturn(false);

        boolean result = interceptor(true).preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(bannedUserService).recordVisitAndCheckIfBanned("203.0.113.10", "user@example.com");
    }

    @Test
    void preHandleRendersBannedViewWhenAddressIsBanned() throws Exception {
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");
        when(bannedUserService.recordVisitAndCheckIfBanned("203.0.113.10", null)).thenReturn(true);
        when(thymeleafViewResolver.resolveViewName("bannedUser", Locale.ENGLISH)).thenReturn(bannedView);

        boolean result = interceptor(true).preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        assertThat(response.getHeader("Retry-After")).isEqualTo("900");
        verify(bannedView).render(eq(java.util.Map.of()), eq(request), eq(response));
    }

    @Test
    void preHandleSendsServiceUnavailableWhenBannedViewCannotBeResolved() throws Exception {
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");
        when(bannedUserService.recordVisitAndCheckIfBanned("203.0.113.10", null)).thenReturn(true);
        when(thymeleafViewResolver.resolveViewName("bannedUser", Locale.ENGLISH)).thenReturn(null);

        boolean result = interceptor(true).preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(bannedView, never()).render(any(), any(), any());
    }

    @Test
    void preHandleAllowsRequestWhenBanServiceFails() throws Exception {
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");
        when(bannedUserService.recordVisitAndCheckIfBanned("203.0.113.10", null))
                .thenThrow(new IllegalStateException("database unavailable"));

        boolean result = interceptor(true).preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(thymeleafViewResolver, never()).resolveViewName(any(), any());
    }

    private BannedUserInterceptor interceptor(boolean enabled) {
        return new BannedUserInterceptor(thymeleafViewResolver, bannedUserService, clientIpResolver, enabled);
    }
}
