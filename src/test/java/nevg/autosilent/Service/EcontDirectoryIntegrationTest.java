package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.CheckoutDeliveryDto;
import nevg.autosilent.Repository.*;
import nevg.autosilent.Service.Exception.OrderCreationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class EcontDirectoryIntegrationTest {
    @Autowired EcontDirectoryService directory;
    @Autowired EcontCityRepository cities;
    @Autowired EcontOfficeRepository offices;
    @Autowired EcontSyncStateRepository states;
    @Autowired CheckoutOrderService checkout;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired OrderAddressRepository addresses;
    @Autowired AdminOrderService adminOrders;
    @MockitoBean EcontClient client;
    final JsonMapper mapper = JsonMapper.builder().build();

    @BeforeEach void resetDirectory() {
        offices.deleteAll(); cities.deleteAll(); states.deleteAll();
        when(client.fetch("getCities", "cities")).thenReturn(mapper.readTree("""
                [{"id":1,"name":"София","regionName":"София-град","postCode":"1000"},
                 {"id":2,"name":"Варна","regionName":"Варна","postCode":"9000"}]
                """));
        when(client.fetch("getOffices", "offices")).thenReturn(mapper.readTree("""
                [{"id":10,"code":"1001","name":"Център","address":{"city":{"id":1},"fullAddress":"ул. Пример 1"}},
                 {"id":11,"code":"1002","name":"Еконтомат","isAPS":true,"address":{"city":{"id":1},"fullAddress":"ул. Пример 2"}}]
                """));
    }

    @Test void refreshImportsCitiesAndOnlyRegularOffices() {
        assertThat(directory.refresh()).isTrue();
        assertThat(directory.cities()).hasSize(2);
        assertThat(directory.offices(1L)).extracting("code").containsExactly("1001");
        assertThat(directory.status().getLastSuccess()).isNotNull();
        assertThat(directory.status().getLastError()).isNull();
    }

    @Test void closedOfficesAreDeactivatedAndExistingOrderSnapshotsStayUnchanged() {
        assertThat(directory.refresh()).isTrue();
        var snapshot = directory.resolve(new CheckoutDeliveryDto(1L, "OFFICE", 10L, null));
        when(client.fetch("getOffices", "offices")).thenReturn(mapper.readTree("""
                [{"id":12,"code":"9001","name":"Нов офис","address":{"city":{"id":2},"fullAddress":"ул. Нова 2"}}]
                """));
        assertThat(directory.refresh()).isTrue();
        assertThat(directory.offices(1L)).isEmpty();
        assertThat(offices.findById(10L).orElseThrow().isActive()).isFalse();
        assertThat(snapshot.getCourierOfficeCode()).isEqualTo("1001");
        assertThat(snapshot.getAddressLine()).isEqualTo("ул. Пример 1");
        assertThatThrownBy(() -> directory.resolve(new CheckoutDeliveryDto(1L, "OFFICE", 10L, null)))
                .isInstanceOf(OrderCreationException.class);
    }

    @Test void malformedResponsesPreservePreviousDirectoryAndLastSuccess() {
        assertThat(directory.refresh()).isTrue();
        var lastSuccess = directory.status().getLastSuccess();
        when(client.fetch("getCities", "cities")).thenReturn(mapper.readTree("""
                [{"id":3,"name":"Changed"}]
                """));
        // Office 10 still points at city 1, absent from this malformed response.
        assertThat(directory.refresh()).isFalse();
        assertThat(directory.cities()).extracting("id").containsExactly(2L, 1L);
        assertThat(directory.offices(1L)).hasSize(1);
        assertThat(directory.status().getLastSuccess()).isEqualTo(lastSuccess);
        assertThat(directory.status().getLastError()).isNotBlank();
    }

    @Test void deliveryRejectsOfficeFromAnotherCityAndValidatesAddress() {
        assertThat(directory.refresh()).isTrue();
        assertThatThrownBy(() -> directory.resolve(new CheckoutDeliveryDto(2L, "OFFICE", 10L, null)))
                .isInstanceOf(OrderCreationException.class);
        assertThatThrownBy(() -> directory.resolve(new CheckoutDeliveryDto(1L, "ADDRESS", null, "a")))
                .isInstanceOf(OrderCreationException.class);
        var address = directory.resolve(new CheckoutDeliveryDto(2L, "ADDRESS", null, "  ул. Пример 5  "));
        assertThat(address.getAddressLine()).isEqualTo("ул. Пример 5");
        assertThat(address.getCourierOfficeCode()).isNull();
    }

    @Test void confirmedOrderPersistsDeliveryAndStockChangesTogether() {
        assertThat(directory.refresh()).isTrue();
        var category = new nevg.autosilent.Models.Entity.Category();
        category.setCategory("Checkout test " + java.util.UUID.randomUUID());
        categories.save(category);
        var product = new nevg.autosilent.Models.Entity.Product();
        product.setNameProduct("Checkout test"); product.setModel(java.util.UUID.randomUUID().toString());
        product.setSku("TEST-" + java.util.UUID.randomUUID()); product.setUrl("test-" + java.util.UUID.randomUUID());
        product.setCategory(category); product.setUser(users.findAll().getFirst());
        product.setStock(5); product.setPrice(new java.math.BigDecimal("12.50"));
        products.save(product);
        var request = new nevg.autosilent.Models.Dto.CartOrderDto(
                java.util.List.of(new nevg.autosilent.Models.Dto.CartItemOrderDto(product.getId(), 2)),
                "Ivan", "Ivanov", "ivan@example.com", "0888123456", "Call first", "",
                new CheckoutDeliveryDto(1L, "OFFICE", 10L, null));
        String number = checkout.create(request, null);
        var order = orders.findByOrderNumber(number).orElseThrow();
        assertThat(order.getDeliveryType()).isEqualTo(nevg.autosilent.Models.Enums.DeliveryType.ECONT_OFFICE);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("25.00");
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
        var snapshot = addresses.findByOrderId(order.getId()).orElseThrow();
        assertThat(snapshot.getCourierOfficeCode()).isEqualTo("1001");
        assertThat(snapshot.getAddressLine()).isEqualTo("ул. Пример 1");
        assertThat(adminOrders.getOrder(order.getId()).delivery().officeCode()).isEqualTo("1001");
        long orderCount = orders.count();
        var invalidStock = new nevg.autosilent.Models.Dto.CartOrderDto(
                java.util.List.of(new nevg.autosilent.Models.Dto.CartItemOrderDto(product.getId(), 99)),
                request.firstName(), request.lastName(), request.email(), request.phone(), "", "", request.delivery());
        assertThatThrownBy(() -> checkout.create(invalidStock, null)).isInstanceOf(OrderCreationException.class);
        assertThat(orders.count()).isEqualTo(orderCount);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
    }
}
