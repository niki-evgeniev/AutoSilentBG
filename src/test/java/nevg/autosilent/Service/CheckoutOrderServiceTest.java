package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.*;
import nevg.autosilent.Models.Entity.*;
import nevg.autosilent.Models.Enums.DeliveryType;
import nevg.autosilent.Repository.*;
import nevg.autosilent.Service.Exception.OrderCreationException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CheckoutOrderServiceTest {
    final OrderService orders = mock(OrderService.class);
    final EcontDirectoryService directory = mock(EcontDirectoryService.class);
    final OrderRepository repository = mock(OrderRepository.class);
    final OrderAddressRepository addresses = mock(OrderAddressRepository.class);
    final CheckoutOrderService service = new CheckoutOrderService(orders, directory, repository, addresses);
    final CartOrderDto request = new CartOrderDto(List.of(new CartItemOrderDto(1L, 2)),
            "Ivan", "Ivanov", "ivan@example.com", "0888123456", "Note", "",
            new CheckoutDeliveryDto(1L, "OFFICE", 10L, null));

    @Test void invalidDeliveryNeverCreatesAnOrderOrChangesStock() {
        when(directory.resolve(request.delivery())).thenThrow(new OrderCreationException("invalid"));
        assertThatThrownBy(() -> service.create(request, null)).isInstanceOf(OrderCreationException.class);
        verifyNoInteractions(orders, repository, addresses);
    }

    @Test void guestOrderSavesValidatedOfficeSnapshotAndEcontDeliveryType() {
        var address = new OrderAddressEntity(); address.setCourierOfficeCode("1001");
        when(directory.resolve(request.delivery())).thenReturn(address);
        when(orders.createGuestOrder(request.email(), request.firstName(), request.lastName(), request.phone(),
                request.customerNote(), request.items(), request.promoCode())).thenReturn("NRT-1");
        var order = new OrderEntity();
        when(repository.findByOrderNumber("NRT-1")).thenReturn(Optional.of(order));
        assertThat(service.create(request, null)).isEqualTo("NRT-1");
        assertThat(order.getDeliveryType()).isEqualTo(DeliveryType.ECONT_OFFICE);
        assertThat(address.getOrder()).isSameAs(order);
        assertThat(order.getAddress()).isSameAs(address);
        verify(addresses).save(address);
    }
}
