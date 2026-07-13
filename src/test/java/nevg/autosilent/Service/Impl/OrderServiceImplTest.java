package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.CartItemOrderDto;
import nevg.autosilent.Models.Dto.QuickOrderDto;
import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Entity.OrderItemEntity;
import nevg.autosilent.Models.Entity.OrderStatusHistoryEntity;
import nevg.autosilent.Models.Entity.Picture;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Service.Exception.OrderCreationException;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.OrderRepository;
import nevg.autosilent.Repository.OrderStatusHistoryRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock OrderStatusHistoryRepository historyRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock MessageSource messageSource;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, orderItemRepository, historyRepository,
                productRepository, userRepository, messageSource);
        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createGuestOrderPersistsOrderItemsHistoryAndReducesStock() {
        Product product = product(3L, "Car / Audio", "SKU-3", "12.50", 10);
        product.addPicture(picture("main image.png", true));
        when(productRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(product));

        String orderNumber = service.createGuestOrder(" guest@example.com ", " Ivan ", " Ivanov ",
                " 0888123456 ", "  call first  ", List.of(new CartItemOrderDto(3L, 2)));

        assertThat(orderNumber).startsWith("NRT-");
        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        OrderEntity order = orderCaptor.getValue();
        assertThat(order.getCustomerFirstName()).isEqualTo("Ivan");
        assertThat(order.getCustomerLastName()).isEqualTo("Ivanov");
        assertThat(order.getCustomerEmail()).isEqualTo("guest@example.com");
        assertThat(order.getCustomerPhone()).isEqualTo("0888123456");
        assertThat(order.getCustomerNote()).isEqualTo("call first");
        assertThat(order.getSubtotalPrice()).isEqualByComparingTo("25.00");
        assertThat(order.getDiscountPrice()).isEqualByComparingTo("0.00");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("25.00");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItemEntity>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).singleElement().satisfies(item -> {
            assertThat(item.getOrder()).isSameAs(order);
            assertThat(item.getProduct()).isSameAs(product);
            assertThat(item.getProductName()).isEqualTo("Car / Audio");
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getUnitPrice()).isEqualByComparingTo("12.50");
            assertThat(item.getTotalPrice()).isEqualByComparingTo("25.00");
            assertThat(item.getProductImageUrl()).isEqualTo("/ProductImages/Car-Audio/main%20image.png");
        });
        assertThat(product.getStock()).isEqualTo(8);
        verify(productRepository).saveAll(List.of(product));

        ArgumentCaptor<OrderStatusHistoryEntity> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getOrder()).isSameAs(order);
        assertThat(historyCaptor.getValue().getNewStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void createGuestOrderMergesDuplicateCartItemsBeforeCheckingStock() {
        Product product = product(3L, "Product", "SKU-3", "5.00", 3);
        when(productRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(product));

        service.createGuestOrder("guest@example.com", "Ivan", "Ivanov", "0888123456", null,
                List.of(new CartItemOrderDto(3L, 1), new CartItemOrderDto(3L, 2)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItemEntity>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).singleElement()
                .satisfies(item -> assertThat(item.getQuantity()).isEqualTo(3));
        assertThat(product.getStock()).isZero();
    }

    @Test
    void createQuickOrderRequiresTwoNames() {
        QuickOrderDto request = new QuickOrderDto(3L, 1, "Ivan", "ivan@example.com", "0888123456");

        assertThatThrownBy(() -> service.createQuickOrder(request))
                .isInstanceOf(OrderCreationException.class);
    }

    @Test
    void createGuestOrderRejectsInsufficientStock() {
        Product product = product(3L, "Product", "SKU-3", "5.00", 1);
        when(productRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.createGuestOrder("guest@example.com", "Ivan", "Ivanov",
                "0888123456", null, List.of(new CartItemOrderDto(3L, 2))))
                .isInstanceOf(OrderCreationException.class);
    }

    @Test
    void getCheckoutCustomerThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCheckoutCustomer("missing@example.com"))
                .isInstanceOf(OrderCreationException.class);
    }

    private Product product(Long id, String name, String sku, String price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setNameProduct(name);
        product.setSku(sku);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        return product;
    }

    private Picture picture(String fileName, boolean mainImage) {
        Picture picture = new Picture();
        picture.setFileName(fileName);
        picture.setMainImage(mainImage);
        return picture;
    }
}
