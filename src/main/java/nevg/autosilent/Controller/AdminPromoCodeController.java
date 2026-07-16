package nevg.autosilent.Controller;

import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.PromoCodeCreateDto;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.Exception.ProductAlreadyExistsException;
import nevg.autosilent.Service.PromoCodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromoCodeController {

    private final PromoCodeService promoCodeService;

    public AdminPromoCodeController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @GetMapping("/admin/settings/promo-codes")
    public ModelAndView promoCodes() {
        return promoCodePage(new PromoCodeCreateDto());
    }

    @PostMapping("/admin/settings/promo-codes")
    public ModelAndView create(@Valid @ModelAttribute("promoCode") PromoCodeCreateDto promoCode,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal ShopUserDetails currentUser,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return promoCodePage(promoCode);
        }

        try {
            promoCodeService.create(promoCode, currentUser.getUsername());
        } catch (ProductAlreadyExistsException exception) {
            bindingResult.rejectValue(exception.getField(), "promoCode.exists", exception.getMessage());
            return promoCodePage(promoCode);
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("discountPercent", "promoCode.discount.invalid", exception.getMessage());
            return promoCodePage(promoCode);
        }

        redirectAttributes.addFlashAttribute("promoCodeCreated", true);
        return new ModelAndView("redirect:/admin/settings/promo-codes");
    }

    private ModelAndView promoCodePage(PromoCodeCreateDto promoCode) {
        ModelAndView modelAndView = new ModelAndView("admin-promo-codes");
        modelAndView.addObject("promoCode", promoCode);
        modelAndView.addObject("promoCodes", promoCodeService.getAll());
        modelAndView.addObject("allowedDiscounts", promoCodeService.allowedDiscounts());
        return modelAndView;
    }
}
