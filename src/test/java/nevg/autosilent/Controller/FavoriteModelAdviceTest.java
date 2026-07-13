package nevg.autosilent.Controller;

import nevg.autosilent.Service.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteModelAdviceTest {

    @Mock
    private FavoriteService favoriteService;

    private FavoriteModelAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new FavoriteModelAdvice(favoriteService);
    }

    @Test
    void classIsControllerAdvice() {
        assertThat(FavoriteModelAdvice.class.getAnnotation(ControllerAdvice.class)).isNotNull();
    }

    @Test
    void favoriteProductIdsIsExposedAsModelAttribute() throws NoSuchMethodException {
        Method method = FavoriteModelAdvice.class.getMethod("favoriteProductIds",
                org.springframework.security.core.Authentication.class);
        ModelAttribute attribute = method.getAnnotation(ModelAttribute.class);

        assertThat(attribute).isNotNull();
        assertThat(attribute.value()).isEqualTo("favoriteProductIds");
    }

    @Test
    void returnsEmptySetWhenAuthenticationIsMissing() {
        Set<Long> result = advice.favoriteProductIds(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(favoriteService);
    }

    @Test
    void returnsEmptySetWhenAuthenticationIsNotAuthenticated() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user@example.com", "password");
        authentication.setAuthenticated(false);

        Set<Long> result = advice.favoriteProductIds(authentication);

        assertThat(result).isEmpty();
        verifyNoInteractions(favoriteService);
    }

    @Test
    void returnsEmptySetForAnonymousAuthentication() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        Set<Long> result = advice.favoriteProductIds(authentication);

        assertThat(result).isEmpty();
        verifyNoInteractions(favoriteService);
    }

    @Test
    void returnsCurrentUsersFavoriteProductIds() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user@example.com", "password", "ROLE_USER");
        when(favoriteService.getFavoriteProductIds("user@example.com")).thenReturn(Set.of(2L, 5L));

        Set<Long> result = advice.favoriteProductIds(authentication);

        assertThat(result).containsExactlyInAnyOrder(2L, 5L);
        verify(favoriteService).getFavoriteProductIds("user@example.com");
    }
}
