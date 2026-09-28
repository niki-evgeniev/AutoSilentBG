package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Service.CatalogSeoService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSeoControllerTest {

    @Mock
    private CatalogSeoService service;

    private CatalogSeoController controller;

    @BeforeEach
    void setUp() {
        controller = new CatalogSeoController(service);
    }

    @Test
    void controllerIsRestrictedToAdmin() {
        PreAuthorize authorization = CatalogSeoController.class.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void editReturnsCatalogSeoForm() {
        SeoDto seo = validSeo();
        when(service.get()).thenReturn(seo);

        ModelAndView result = controller.edit();

        assertThat(result.getViewName()).isEqualTo("catalog-seo");
        assertThat(result.getModel().get("seo")).isSameAs(seo);
    }

    @Test
    void savePersistsSeoAndRedirects() {
        SeoDto seo = validSeo();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        ModelAndView result = controller.save(
                seo, new BeanPropertyBindingResult(seo, "seo"), redirect);

        verify(service).save(seo);
        assertThat(result.getViewName()).isEqualTo("redirect:/admin/settings/catalog-seo");
        assertThat(redirect.getFlashAttributes()).containsKey("seoUpdated");
    }

    private SeoDto validSeo() {
        SeoDto dto = new SeoDto();
        dto.setProductName("Каталог /shumoizolaciya");
        dto.setTitle("Catalog title");
        dto.setDescription("Catalog description");
        return dto;
    }
}
