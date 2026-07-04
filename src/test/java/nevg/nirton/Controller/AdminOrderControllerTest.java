package nevg.nirton.Controller;

import nevg.nirton.Models.Dto.AdminOrderDetailDto;
import nevg.nirton.Models.Dto.AdminOrderSummaryDto;
import nevg.nirton.Models.Enums.DeliveryType;
import nevg.nirton.Models.Enums.OrderStatus;
import nevg.nirton.Models.Enums.PaymentMethod;
import nevg.nirton.Models.Enums.PaymentStatus;
import nevg.nirton.Models.Security.ShopUserDetails;
import nevg.nirton.Service.AdminOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private AdminOrderService adminOrderService;

    private AdminOrderController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminOrderController(adminOrderService);
    }

    @Test
    void controllerIsRestrictedToAdminAndModerator() {
        PreAuthorize authorization = AdminOrderController.class.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasAnyRole('ADMIN', 'MODERATOR')");
    }

    @Test
    void ordersReturnsAllOrdersInModel() {
        List<AdminOrderSummaryDto> orders = List.of(new AdminOrderSummaryDto(
                1L, "NRT-1", "Ivan Ivanov", "0888123456", OrderStatus.NEW,
                new BigDecimal("25.00"), LocalDateTime.of(2026, 7, 4, 12, 0), false
        ));
        when(adminOrderService.getAllOrders()).thenReturn(orders);

        ModelAndView result = controller.orders();

        assertThat(result.getViewName()).isEqualTo("admin-orders");
        assertThat(result.getModel().get("orders")).isSameAs(orders);
    }

    @Test
    void orderReturnsDetailsAndAvailableStatuses() {
        AdminOrderDetailDto order = orderDetails();
        when(adminOrderService.getOrder(5L)).thenReturn(order);

        ModelAndView result = controller.order(5L);

        assertThat(result.getViewName()).isEqualTo("admin-order-details");
        assertThat(result.getModel().get("order")).isSameAs(order);
        assertThat((OrderStatus[]) result.getModel().get("statuses"))
                .containsExactly(OrderStatus.values());
    }

    @Test
    void updateStatusDelegatesToServiceAndRedirects() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView result = controller.updateStatus(
                5L, OrderStatus.SHIPPED, "Sent with courier", moderator(), redirectAttributes);

        verify(adminOrderService).updateStatus(
                5L, OrderStatus.SHIPPED, "Sent with courier", "moderator@nirton.bg");
        assertThat(result.getViewName()).isEqualTo("redirect:/admin/orders/5");
        assertThat(redirectAttributes.getFlashAttributes().get("statusUpdated")).isEqualTo(true);
    }

    private AdminOrderDetailDto orderDetails() {
        return new AdminOrderDetailDto(
                5L, "NRT-5", "Ivan", "Ivanov", "ivan@example.com", "0888123456",
                false, DeliveryType.STORE_PICKUP, PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING, OrderStatus.NEW,
                new BigDecimal("25.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("25.00"), null, null,
                LocalDateTime.of(2026, 7, 4, 12, 0), List.of(), List.of()
        );
    }

    private ShopUserDetails moderator() {
        return new ShopUserDetails(
                "moderator@nirton.bg", "password", "Moderator",
                List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"))
        );
    }
}
