package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.CartItemOrderDto;
import nevg.autosilent.Models.Dto.CartOrderDto;
import nevg.autosilent.Models.Dto.CheckoutCustomerDto;
import nevg.autosilent.Models.Dto.PromoCodeApplyDto;
import nevg.autosilent.Models.Dto.PromoCodeValidationDto;
import nevg.autosilent.Models.Dto.QuickOrderDto;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.Exception.OrderCreationException;
import nevg.autosilent.Service.OrderService;
import nevg.autosilent.Service.PromoCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private PromoCodeService promoCodeService;

    @Mock
    private MessageSource messageSource;

    private OrderController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderController(orderService, promoCodeService, messageSource);
    }

    @Test
    void cartOrderCreatesGuestOrderWithoutAuthenticatedUser() {
        CartOrderDto request = cartOrder();
        when(orderService.createGuestOrder(
                "ivan@example.com", "Ivan", "Ivanov", "0888123456", "Call first", request.items(), "SAVE10"))
                .thenReturn("NRT-GUEST");

        var response = controller.createCartOrder(request, null, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("orderNumber", "NRT-GUEST");
    }

    @Test
    void cartOrderCreatesOrderAndReturnsSuccessUrl() {
        CartOrderDto request = cartOrder();
        when(orderService.createRegisteredOrder(
                "user@example.com", "Ivan", "Ivanov", "0888123456", "Call first", request.items(), "SAVE10"))
                .thenReturn("NRT-123");

        var response = controller.createCartOrder(request, currentUser(), Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("orderNumber", "NRT-123")
                .containsEntry("redirectUrl", "/orders/success/NRT-123");
    }

    @Test
    void cartOrderReturnsBadRequestWhenServiceRejectsOrder() {
        CartOrderDto request = cartOrder();
        doThrow(new OrderCreationException("Out of stock"))
                .when(orderService).createRegisteredOrder(
                        "user@example.com", "Ivan", "Ivanov", "0888123456", "Call first", request.items(), "SAVE10");

        var response = controller.createCartOrder(request, currentUser(), Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Out of stock");
    }

    @Test
    void checkoutReturnsCustomerInModel() {
        CheckoutCustomerDto customer = new CheckoutCustomerDto(
                "Ivan", "Ivanov", "user@example.com", "0888123456");
        when(orderService.getCheckoutCustomer("user@example.com")).thenReturn(customer);

        ModelAndView result = controller.checkout(currentUser());

        assertThat(result.getViewName()).isEqualTo("checkout");
        assertThat(result.getModel().get("customer")).isSameAs(customer);
        assertThat(result.getModel().get("guestCheckout")).isEqualTo(false);
    }

    @Test
    void checkoutSupportsGuestCustomer() {
        ModelAndView result = controller.checkout(null);

        CheckoutCustomerDto customer = (CheckoutCustomerDto) result.getModel().get("customer");
        assertThat(result.getViewName()).isEqualTo("checkout");
        assertThat(customer.email()).isEmpty();
        assertThat(result.getModel().get("guestCheckout")).isEqualTo(true);
    }

    @Test
    void promoCodePreviewUsesOnlyPromoDiscountForLoggedInUsers() {
        when(promoCodeService.discountPercent("NIKI89")).thenReturn(new BigDecimal("30"));
        when(promoCodeService.normalizeCode("NIKI89")).thenReturn("NIKI89");

        var response = controller.validatePromoCode(new PromoCodeApplyDto("NIKI89"), currentUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(PromoCodeValidationDto.class)
                .satisfies(body -> {
                    PromoCodeValidationDto dto = (PromoCodeValidationDto) body;
                    assertThat(dto.promoCode()).isEqualTo("NIKI89");
                    assertThat(dto.promoDiscountPercent()).isEqualByComparingTo("30");
                    assertThat(dto.totalDiscountPercent()).isEqualByComparingTo("30");
                });
        verify(orderService, never()).getCheckoutCustomer("user@example.com");
    }

    @Test
    void invalidQuickOrderRedirectsBackWithLocalizedError() {
        QuickOrderDto request = quickOrder();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "quickOrderDto");
        bindingResult.rejectValue("phone", "invalid");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(messageSource.getMessage("order.error.invalidQuickOrder", null, Locale.ENGLISH))
                .thenReturn("Check the form");

        ModelAndView result = controller.createQuickOrder(
                request, bindingResult, redirectAttributes, Locale.ENGLISH);

        assertThat(result.getViewName()).isEqualTo("redirect:/shumoizolaciya/7");
        assertThat(redirectAttributes.getFlashAttributes().get("orderError")).isEqualTo("Check the form");
        verify(orderService, never()).createQuickOrder(request);
    }

    @Test
    void quickOrderRedirectsToSuccessPage() {
        QuickOrderDto request = quickOrder();
        when(orderService.createQuickOrder(request)).thenReturn("NRT-456");

        ModelAndView result = controller.createQuickOrder(
                request, new BeanPropertyBindingResult(request, "quickOrderDto"),
                new RedirectAttributesModelMap(), Locale.ENGLISH);

        assertThat(result.getViewName()).isEqualTo("redirect:/orders/success/NRT-456");
    }

    @Test
    void rejectedQuickOrderRedirectsBackWithServiceError() {
        QuickOrderDto request = quickOrder();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new OrderCreationException("Product unavailable"))
                .when(orderService).createQuickOrder(request);

        ModelAndView result = controller.createQuickOrder(
                request, new BeanPropertyBindingResult(request, "quickOrderDto"),
                redirectAttributes, Locale.ENGLISH);

        assertThat(result.getViewName()).isEqualTo("redirect:/shumoizolaciya/7");
        assertThat(redirectAttributes.getFlashAttributes().get("orderError"))
                .isEqualTo("Product unavailable");
    }

    @Test
    void successReturnsOrderNumberInModel() {
        ModelAndView result = controller.success("NRT-789");

        assertThat(result.getViewName()).isEqualTo("order-success");
        assertThat(result.getModel().get("orderNumber")).isEqualTo("NRT-789");
    }

    private CartOrderDto cartOrder() {
        return new CartOrderDto(
                List.of(new CartItemOrderDto(7L, 2)),
                "Ivan", "Ivanov", "ivan@example.com", "0888123456", "Call first", "SAVE10");
    }

    private QuickOrderDto quickOrder() {
        return new QuickOrderDto(7L, 1, "Ivan Ivanov", "ivan@example.com", "0888123456");
    }

    private ShopUserDetails currentUser() {
        return new ShopUserDetails("user@example.com", "password", "Ivan", List.of());
    }
}
