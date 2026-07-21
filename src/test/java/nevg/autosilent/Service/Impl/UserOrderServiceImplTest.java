package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Models.Enums.OrderStatus;
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

    private UserOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserOrderServiceImpl(userRepository, orderRepository);
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
}
