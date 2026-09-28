package nevg.autosilent.Controller;

import nevg.autosilent.Service.PromoCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminPromoCodeControllerTest {

    @Mock
    private PromoCodeService service;

    private AdminPromoCodeController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminPromoCodeController(service);
    }

    @Test
    void controllerIsRestrictedToAdmin() {
        PreAuthorize authorization = AdminPromoCodeController.class.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void updateDiscountDelegatesToServiceAndRedirects() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView result = controller.updateDiscount(7L, 20, redirectAttributes);

        verify(service).updateDiscount(7L, 20);
        assertThat(result.getViewName()).isEqualTo("redirect:/admin/settings/promo-codes");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("promoCodeUpdated");
    }

    @Test
    void deleteDelegatesToServiceAndRedirects() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView result = controller.delete(7L, redirectAttributes);

        verify(service).delete(7L);
        assertThat(result.getViewName()).isEqualTo("redirect:/admin/settings/promo-codes");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("promoCodeDeleted");
    }
}
