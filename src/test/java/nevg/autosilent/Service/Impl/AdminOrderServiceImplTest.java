package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Entity.OrderItemEntity;
import nevg.autosilent.Models.Entity.OrderStatusHistoryEntity;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Models.Enums.DeliveryType;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Models.Enums.PaymentMethod;
import nevg.autosilent.Models.Enums.PaymentStatus;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.OrderRepository;
import nevg.autosilent.Repository.OrderStatusHistoryRepository;
import nevg.autosilent.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock OrderStatusHistoryRepository historyRepository;
    @Mock UserRepository userRepository;
    @Mock MessageSource messageSource;
    private AdminOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminOrderServiceImpl(orderRepository, orderItemRepository,
                historyRepository, userRepository, messageSource);
    }

    @Test
    void getAllOrdersMapsSummaryAndGuestStatus() {
        OrderEntity order = order();
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        var result = service.getAllOrders();

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.orderNumber()).isEqualTo("NRT-10");
            assertThat(dto.customerName()).isEqualTo("Ivan Ivanov");
            assertThat(dto.guestOrder()).isTrue();
            assertThat(dto.totalPrice()).isEqualByComparingTo("25.00");
        });
    }

    @Test
    void searchOrdersByNumberTrimsQueryAndMapsResults() {
        OrderEntity order = order();
        when(orderRepository.findByOrderNumberContainingIgnoreCaseOrderByCreatedAtDesc("NRT-10"))
                .thenReturn(List.of(order));

        var result = service.searchOrdersByNumber("  NRT-10  ");

        assertThat(result).singleElement()
                .extracting(dto -> dto.orderNumber())
                .isEqualTo("NRT-10");
        verify(orderRepository)
                .findByOrderNumberContainingIgnoreCaseOrderByCreatedAtDesc("NRT-10");
    }

    @Test
    void blankOrderSearchReturnsAllOrders() {
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(service.searchOrdersByNumber("   ")).isEmpty();

        verify(orderRepository).findAllByOrderByCreatedAtDesc();
        verify(orderRepository, never())
                .findByOrderNumberContainingIgnoreCaseOrderByCreatedAtDesc(
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void filterOrdersByStatusReturnsOnlySelectedStatus() {
        OrderEntity order = order();
        order.setOrderStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByOrderStatusOrderByCreatedAtDesc(OrderStatus.SHIPPED))
                .thenReturn(List.of(order));

        var result = service.filterOrders("", OrderStatus.SHIPPED);

        assertThat(result).singleElement()
                .extracting(dto -> dto.status())
                .isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void filterOrdersCombinesNumberAndStatus() {
        when(orderRepository
                .findByOrderNumberContainingIgnoreCaseAndOrderStatusOrderByCreatedAtDesc(
                        "NRT", OrderStatus.NEW))
                .thenReturn(List.of(order()));

        assertThat(service.filterOrders(" NRT ", OrderStatus.NEW)).hasSize(1);

        verify(orderRepository)
                .findByOrderNumberContainingIgnoreCaseAndOrderStatusOrderByCreatedAtDesc(
                        "NRT", OrderStatus.NEW);
    }

    @Test
    void getOrderMapsItemsAndHistory() {
        OrderEntity order = order();
        OrderItemEntity item = new OrderItemEntity();
        item.setProductName("Product");
        item.setProductSku("SKU-1");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setTotalPrice(new BigDecimal("20.00"));
        User moderator = user("moderator@example.com");
        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOldStatus(OrderStatus.NEW);
        history.setNewStatus(OrderStatus.CONFIRMED);
        history.setChangedByUser(moderator);
        history.setNote("Confirmed");
        history.setChangedAt(LocalDateTime.of(2026, 7, 7, 10, 0));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(item));
        when(historyRepository.findByOrderIdOrderByChangedAtDesc(10L)).thenReturn(List.of(history));

        var result = service.getOrder(10L);

        assertThat(result.items()).singleElement().satisfies(dto -> {
            assertThat(dto.productName()).isEqualTo("Product");
            assertThat(dto.quantity()).isEqualTo(2);
        });
        assertThat(result.history()).singleElement().satisfies(dto -> {
            assertThat(dto.changedBy()).isEqualTo("moderator@example.com");
            assertThat(dto.newStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });
    }

    @Test
    void updateStatusSetsDeliveredOrderAsPaidAndCreatesHistory() {
        OrderEntity order = order();
        User moderator = user("moderator@example.com");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailIgnoreCase("moderator@example.com")).thenReturn(Optional.of(moderator));

        service.updateStatus(10L, OrderStatus.DELIVERED, "  Delivered safely  ", "moderator@example.com");

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getAdminNote()).isEqualTo("Delivered safely");
        verify(orderRepository).save(order);
        ArgumentCaptor<OrderStatusHistoryEntity> captor = ArgumentCaptor.forClass(OrderStatusHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getOldStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(captor.getValue().getNewStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(captor.getValue().getNote()).isEqualTo("Delivered safely");
        assertThat(captor.getValue().getChangedByUser()).isSameAs(moderator);
    }

    @Test
    void updateStatusDoesNothingWhenStatusIsUnchanged() {
        OrderEntity order = order();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        service.updateStatus(10L, OrderStatus.NEW, "Ignored", "moderator@example.com");

        verify(userRepository, never()).findByEmailIgnoreCase(org.mockito.ArgumentMatchers.anyString());
        verify(orderRepository, never()).save(order);
        verify(historyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateStatusRejectsUnknownModerator() {
        OrderEntity order = order();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(
                10L, OrderStatus.CONFIRMED, null, "missing@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.NEW);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void missingOrderReturnsLocalizedNotFound() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());
        when(messageSource.getMessage(
                org.mockito.ArgumentMatchers.eq("admin.order.notFound"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("Order not found");

        assertThatThrownBy(() -> service.getOrder(404L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("Order not found");
    }

    @Test
    void orderDetailsCalculatesHistoricalDiscountPercent() {
        var dto = new nevg.autosilent.Models.Dto.AdminOrderDetailDto(
                1L, "NRT-1", "Ivan", "Ivanov", "ivan@example.com", "0888123456", false,
                DeliveryType.STORE_PICKUP, PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING,
                OrderStatus.NEW, new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("20.00"), null, BigDecimal.ZERO, new BigDecimal("80.00"), null, null,
                LocalDateTime.now(), List.of(), List.of());

        assertThat(dto.discountPercent()).isEqualByComparingTo("20");
    }

    @Test
    void orderDetailsFormatsDiscountPercentWithoutScientificNotation() {
        var dto = new nevg.autosilent.Models.Dto.AdminOrderDetailDto(
                1L, "NRT-1", "Ivan", "Ivanov", "ivan@example.com", "0888123456", false,
                DeliveryType.STORE_PICKUP, PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING,
                OrderStatus.NEW, new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("30.00"), null, BigDecimal.ZERO, new BigDecimal("70.00"), null, null,
                LocalDateTime.now(), List.of(), List.of());

        assertThat(dto.discountPercentText()).isEqualTo("30");
    }

    private OrderEntity order() {
        OrderEntity order = new OrderEntity();
        order.setId(10L);
        order.setOrderNumber("NRT-10");
        order.setCustomerFirstName("Ivan");
        order.setCustomerLastName("Ivanov");
        order.setCustomerEmail("ivan@example.com");
        order.setCustomerPhone("0888123456");
        order.setDeliveryType(DeliveryType.STORE_PICKUP);
        order.setDeliveryPrice(BigDecimal.ZERO);
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.NEW);
        order.setSubtotalPrice(new BigDecimal("25.00"));
        order.setDiscountPrice(BigDecimal.ZERO);
        order.setTotalPrice(new BigDecimal("25.00"));
        order.setCreatedAt(LocalDateTime.of(2026, 7, 7, 9, 0));
        return order;
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }
}
