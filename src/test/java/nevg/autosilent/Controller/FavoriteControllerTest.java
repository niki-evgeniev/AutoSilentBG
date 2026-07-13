package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.ProductViewDto;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    private FavoriteController controller;
    private ShopUserDetails user;

    @BeforeEach
    void setUp() {
        controller = new FavoriteController(favoriteService);
        user = new ShopUserDetails("user@example.com", "password", "Ivan", List.of());
    }

    @Test
    void controllerRequiresAuthenticatedUser() {
        PreAuthorize authorization = FavoriteController.class.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("isAuthenticated()");
    }

    @Test
    void favoritesEndpointIsMappedToFavoritesPage() throws NoSuchMethodException {
        Method method = FavoriteController.class.getMethod("favorites", ShopUserDetails.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/favorites");
    }

    @Test
    void toggleEndpointUsesFavoritesAsDefaultSource() throws NoSuchMethodException {
        Method method = FavoriteController.class.getMethod("toggle", Long.class, String.class, ShopUserDetails.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        RequestParam source = method.getParameters()[1].getAnnotation(RequestParam.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/favorites/{productId}/toggle");
        assertThat(source).isNotNull();
        assertThat(source.defaultValue()).isEqualTo("favorites");
    }

    @Test
    void favoritesReturnsCurrentUsersProducts() {
        List<ProductViewDto> products = List.of(product());
        when(favoriteService.getFavorites("user@example.com")).thenReturn(products);

        ModelAndView result = controller.favorites(user);

        assertThat(result.getViewName()).isEqualTo("favorites");
        assertThat(result.getModel().get("products")).isSameAs(products);
        verify(favoriteService).getFavorites("user@example.com");
    }

    @Test
    void favoritesSupportsEmptyList() {
        when(favoriteService.getFavorites("user@example.com")).thenReturn(List.of());

        ModelAndView result = controller.favorites(user);

        assertThat(result.getModel().get("products")).isEqualTo(List.of());
    }

    @ParameterizedTest
    @MethodSource("redirectCases")
    void toggleDelegatesToServiceAndUsesSafeRedirect(String source, String expectedRedirect) {
        ModelAndView result = controller.toggle(27L, source, user);

        verify(favoriteService).toggle("user@example.com", 27L);
        assertThat(result.getViewName()).isEqualTo(expectedRedirect);
    }

    private static Stream<Arguments> redirectCases() {
        return Stream.of(
                Arguments.of("products", "redirect:/products"),
                Arguments.of("details", "redirect:/products/27"),
                Arguments.of("favorites", "redirect:/favorites"),
                Arguments.of("unknown", "redirect:/favorites"),
                Arguments.of("https://example.com", "redirect:/favorites"),
                Arguments.of("", "redirect:/favorites")
        );
    }

    private ProductViewDto product() {
        return new ProductViewDto(
                27L, "Product", "SKU-27", "Category", new BigDecimal("19.90"),
                "Product description", 4, "/ProductImages/product/main.jpg"
        );
    }
}
