package nevg.autosilent.Controller;

import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Service.SeoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class SeoController {

    private final SeoService seoService;

    public SeoController(SeoService seoService) {
        this.seoService = seoService;
    }

    @GetMapping("/products/{productId}/seo")
    public ModelAndView edit(@PathVariable Long productId) {
        return form(productId, seoService.getForEdit(productId));
    }

    @PostMapping("/products/{productId}/seo")
    public ModelAndView save(@PathVariable Long productId,
                             @Valid @ModelAttribute("seo") SeoDto seo,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            SeoDto stored = seoService.getForEdit(productId);
            seo.setProductName(stored.getProductName());
            return form(productId, seo);
        }
        seoService.save(productId, seo);
        redirectAttributes.addFlashAttribute("seoUpdated", true);
        return new ModelAndView("redirect:/products/" + productId + "/seo");
    }

    private ModelAndView form(Long productId, SeoDto seo) {
        ModelAndView result = new ModelAndView("product-seo");
        result.addObject("seo", seo);
        result.addObject("productId", productId);
        return result;
    }
}
