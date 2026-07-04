package nevg.nirton.Controller;

import jakarta.validation.Valid;
import nevg.nirton.Models.Dto.CartOrderDto;
import nevg.nirton.Models.Dto.QuickOrderDto;
import nevg.nirton.Models.Security.ShopUserDetails;
import nevg.nirton.Service.Exception.OrderCreationException;
import nevg.nirton.Service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Locale;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final MessageSource messageSource;

    public OrderController(OrderService orderService, MessageSource messageSource) {
        this.orderService = orderService;
        this.messageSource = messageSource;
    }

    @PostMapping("/cart")
    @ResponseBody
    public ResponseEntity<Map<String, String>> createCartOrder(
            @Valid @RequestBody CartOrderDto request,
            @AuthenticationPrincipal ShopUserDetails currentUser,
            Locale locale) {
        if (currentUser == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", messageSource.getMessage("order.error.loginRequired", null, locale)));
        }
        try {
            String orderNumber = orderService.createRegisteredOrder(
                    currentUser.getUsername(), request.firstName(), request.lastName(),
                    request.phone(), request.customerNote(), request.items());
            return ResponseEntity.ok(Map.of(
                    "orderNumber", orderNumber,
                    "redirectUrl", "/orders/success/" + orderNumber));
        } catch (OrderCreationException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/checkout")
    public ModelAndView checkout(@AuthenticationPrincipal ShopUserDetails currentUser) {
        ModelAndView modelAndView = new ModelAndView("checkout");
        modelAndView.addObject("customer", orderService.getCheckoutCustomer(currentUser.getUsername()));
        return modelAndView;
    }

    @PostMapping("/quick")
    public ModelAndView createQuickOrder(@Valid @ModelAttribute QuickOrderDto request,
                                         BindingResult bindingResult,
                                         RedirectAttributes redirectAttributes,
                                         Locale locale) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("orderError",
                    messageSource.getMessage("order.error.invalidQuickOrder", null, locale));
            return new ModelAndView("redirect:/products/" + request.productId());
        }
        try {
            String orderNumber = orderService.createQuickOrder(request);
            return new ModelAndView("redirect:/orders/success/" + orderNumber);
        } catch (OrderCreationException exception) {
            redirectAttributes.addFlashAttribute("orderError", exception.getMessage());
            return new ModelAndView("redirect:/products/" + request.productId());
        }
    }

    @GetMapping("/success/{orderNumber}")
    public ModelAndView success(@PathVariable String orderNumber) {
        ModelAndView modelAndView = new ModelAndView("order-success");
        modelAndView.addObject("orderNumber", orderNumber);
        return modelAndView;
    }
}
