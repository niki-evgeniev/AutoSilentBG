package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Entity.OrderItemEntity;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Models.Enums.DeliveryType;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Models.Enums.PaymentMethod;
import nevg.autosilent.Models.Enums.PaymentStatus;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.OrderRepository;
import nevg.autosilent.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOrderServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;

    private UserOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserOrderServiceImpl(userRepository, orderRepository, orderItemRepository);
    }

    @Test
    void returnsOnlyOrdersBelongingToCurrentUser() {
        User user = new User();
        user.setId(42L);
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("NRT-123");
        order.setOrderStatus(OrderStatus.PROCESSING);
        order.setTotalPrice(new BigDecimal("125.50"));
        order.setCreatedAt(LocalDateTime.of(2026, 7, 18, 12, 30));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(42L)).thenReturn(List.of(order));

        var result = service.getOrders("user@example.com");

        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.orderNumber()).isEqualTo("NRT-123");
            assertThat(summary.status()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(summary.totalPrice()).isEqualByComparingTo("125.50");
            assertThat(summary.createdAt()).isEqualTo(LocalDateTime.of(2026, 7, 18, 12, 30));
        });
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(42L);
    }

    @Test
    void returnsOrderDetailsAndItemsForCurrentUser() {
        User user = new User();
        user.setId(42L);
        OrderEntity order = order(7L);
        OrderItemEntity item = new OrderItemEntity();
        item.setProductName("Sound insulation");
        item.setProductSku("SKU-1");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setTotalPrice(new BigDecimal("40.00"));
        item.setProductImageUrl("/image.png");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByOrderNumberAndUserId("NRT-123", 42L))
                .thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(7L)).thenReturn(List.of(item));

        var result = service.getOrder("NRT-123", "user@example.com");

        assertThat(result.orderNumber()).isEqualTo("NRT-123");
        assertThat(result.items()).singleElement().satisfies(orderItem -> {
            assertThat(orderItem.productName()).isEqualTo("Sound insulation");
            assertThat(orderItem.quantity()).isEqualTo(2);
            assertThat(orderItem.totalPrice()).isEqualByComparingTo("40.00");
        });
        verify(orderRepository).findByOrderNumberAndUserId("NRT-123", 42L);
    }

    @Test
    void foreignOrderIsHiddenAsNotFound() {
        User user = new User();
        user.setId(42L);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByOrderNumberAndUserId("OTHER-ORDER", 42L))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.getOrder("OTHER-ORDER", "user@example.com"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .satisfies(exception -> assertThat(
                        ((org.springframework.web.server.ResponseStatusException) exception)
                                .getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND));
    }

    private OrderEntity order(Long id) {
        OrderEntity order = new OrderEntity();
        order.setId(id);
        order.setOrderNumber("NRT-123");
        order.setOrderStatus(OrderStatus.PROCESSING);
        order.setDeliveryType(DeliveryType.SPEEDY_OFFICE);
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setSubtotalPrice(new BigDecimal("40.00"));
        order.setDeliveryPrice(new BigDecimal("5.00"));
        order.setDiscountPrice(BigDecimal.ZERO);
        order.setPromoDiscountPercent(BigDecimal.ZERO);
        order.setTotalPrice(new BigDecimal("45.00"));
        order.setCreatedAt(LocalDateTime.of(2026, 7, 18, 12, 30));
        return order;
    }
}
