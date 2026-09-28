package nevg.autosilent.Controller;

import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Service.CatalogSeoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class CatalogSeoController {

    private final CatalogSeoService catalogSeoService;

    public CatalogSeoController(CatalogSeoService catalogSeoService) {
        this.catalogSeoService = catalogSeoService;
    }

    @GetMapping("/admin/settings/catalog-seo")
    public ModelAndView edit() {
        return form(catalogSeoService.get());
    }

    @PostMapping("/admin/settings/catalog-seo")
    public ModelAndView save(@Valid @ModelAttribute("seo") SeoDto seo,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            seo.setProductName(catalogSeoService.get().getProductName());
            return form(seo);
        }
        catalogSeoService.save(seo);
        redirectAttributes.addFlashAttribute("seoUpdated", true);
        return new ModelAndView("redirect:/admin/settings/catalog-seo");
    }

    private ModelAndView form(SeoDto seo) {
        ModelAndView result = new ModelAndView("catalog-seo");
        result.addObject("seo", seo);
        return result;
    }
}
