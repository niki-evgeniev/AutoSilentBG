package nevg.autosilent.Controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.ManualOrderPriceService;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.Locale;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderPriceController {
    private final ManualOrderPriceService prices;
    private final MessageSource messages;
    public AdminOrderPriceController(ManualOrderPriceService prices, MessageSource messages) {
        this.prices = prices; this.messages = messages;
    }
    public record Price(@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal total) {}

    @PostMapping("/admin/orders/{id}/price")
    public String update(@PathVariable Long id, @Valid @ModelAttribute Price price, BindingResult errors,
                         @AuthenticationPrincipal ShopUserDetails user, RedirectAttributes attributes, Locale locale) {
        if (errors.hasErrors()) {
            attributes.addFlashAttribute("priceError", messages.getMessage("order.price.invalid", null, locale));
        } else {
            try { prices.update(id, price.total(), user.getUsername()); attributes.addFlashAttribute("priceUpdated", true); }
            catch (IllegalArgumentException exception) { attributes.addFlashAttribute("priceError", exception.getMessage()); }
        }
        return "redirect:/admin/orders/" + id;
    }
}
