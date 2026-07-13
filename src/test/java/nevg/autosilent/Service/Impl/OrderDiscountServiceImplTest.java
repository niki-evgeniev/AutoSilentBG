package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.CartItemOrderDto;
import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.OrderRepository;
import nevg.autosilent.Repository.OrderStatusHistoryRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDiscountServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock OrderStatusHistoryRepository historyRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock MessageSource messageSource;

    @Test
    void registeredOrderAppliesUsersDiscountOnServer() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setDiscountPercent(new BigDecimal("20.00"));
        Product product = new Product();
        product.setId(3L); product.setNameProduct("Product"); product.setSku("SKU-3");
        product.setPrice(new BigDecimal("12.50")); product.setStock(10);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(product));

        service().createRegisteredOrder("user@example.com", "Ivan", "Ivanov", "0888123456", null,
                List.of(new CartItemOrderDto(3L, 2)));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getSubtotalPrice()).isEqualByComparingTo("25.00");
        assertThat(captor.getValue().getDiscountPrice()).isEqualByComparingTo("5.00");
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo("20.00");
    }

    @Test
    void checkoutCustomerContainsConfiguredDiscount() {
        User user = new User();
        user.setEmail("user@example.com"); user.setDiscountPercent(new BigDecimal("10.00"));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        var customer = service().getCheckoutCustomer("user@example.com");

        assertThat(customer.discountPercent()).isEqualByComparingTo("10.00");
    }

    private OrderServiceImpl service() {
        return new OrderServiceImpl(orderRepository, orderItemRepository, historyRepository,
                productRepository, userRepository, messageSource);
    }
}
