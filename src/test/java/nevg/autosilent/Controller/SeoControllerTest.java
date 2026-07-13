package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Service.SeoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeoControllerTest {

    @Mock SeoService seoService;
    private SeoController controller;

    @BeforeEach
    void setUp() {
        controller = new SeoController(seoService);
    }

    @Test
    void controllerIsRestrictedToAdminAndModerator() {
        PreAuthorize authorization = SeoController.class.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasAnyRole('ADMIN', 'MODERATOR')");
    }

    @Test
    void editReturnsStoredSeoAndProductId() {
        SeoDto seo = seo("Product");
        when(seoService.getForEdit(12L)).thenReturn(seo);

        ModelAndView result = controller.edit(12L);

        assertThat(result.getViewName()).isEqualTo("product-seo");
        assertThat(result.getModel().get("seo")).isSameAs(seo);
        assertThat(result.getModel().get("productId")).isEqualTo(12L);
    }

    @Test
    void savePersistsSeoAndRedirectsWithSuccessMessage() {
        SeoDto seo = seo("Product");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        ModelAndView result = controller.save(12L, seo,
                new BeanPropertyBindingResult(seo, "seo"), redirect);

        verify(seoService).save(12L, seo);
        assertThat(result.getViewName()).isEqualTo("redirect:/products/12/seo");
        assertThat(redirect.getFlashAttributes().get("seoUpdated")).isEqualTo(true);
    }

    @Test
    void invalidSeoReturnsFormAndRestoresTrustedProductName() {
        SeoDto submitted = seo("Manipulated name");
        SeoDto stored = seo("Stored product");
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(submitted, "seo");
        binding.rejectValue("title", "invalid");
        when(seoService.getForEdit(12L)).thenReturn(stored);

        ModelAndView result = controller.save(
                12L, submitted, binding, new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("product-seo");
        assertThat(result.getModel().get("seo")).isSameAs(submitted);
        assertThat(submitted.getProductName()).isEqualTo("Stored product");
        assertThat(result.getModel().get("productId")).isEqualTo(12L);
        verify(seoService, never()).save(12L, submitted);
    }

    private SeoDto seo(String productName) {
        SeoDto dto = new SeoDto();
        dto.setProductName(productName);
        dto.setTitle("SEO title");
        dto.setDescription("SEO description");
        return dto;
    }
}
