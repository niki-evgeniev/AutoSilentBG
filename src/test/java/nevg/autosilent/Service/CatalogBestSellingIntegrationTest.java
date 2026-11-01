package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.ProductFilterDto;
import nevg.autosilent.Models.Dto.ProductViewDto;
import nevg.autosilent.Models.Entity.*;
import nevg.autosilent.Models.Enums.*;
import nevg.autosilent.Repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CatalogBestSellingIntegrationTest {
    @Autowired ProductService catalog;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired PromotionRepository promotions;

    @Test
    void ranksDeliveredUnitsAcrossPagesAndPreservesFiltersAndPromotionVisibility() {
        var owner = new User();
        owner.setEmail(UUID.randomUUID() + "@example.com");
        owner.setPassword("test");
        users.saveAndFlush(owner);
        var category = new Category();
        category.setCategory(UUID.randomUUID().toString());
        categories.saveAndFlush(category);
        var pack = product("Z pack", owner, category);
        var regular = product("Y regular", owner, category);
        var cancelled = product("A cancelled", owner, category);
        cancelled.setSold(9999); // Old counters must not influence the ranking.
        var returned = product("B returned", owner, category);
        var pending = product("C pending", owner, category);
        var hidden = product("Hidden", owner, category);
        var inactive = product("Inactive", owner, category);
        inactive.setActive(false);
        for (int i = 0; i < 6; i++) product("D unsold " + i, owner, category);
        sale(pack, OrderStatus.DELIVERED, 2, 25);
        sale(regular, OrderStatus.DELIVERED, 30, 1);
        sale(regular, OrderStatus.DELIVERED, 5, 1);
        sale(cancelled, OrderStatus.CANCELLED, 999, 1);
        sale(returned, OrderStatus.RETURNED, 999, 1);
        sale(pending, OrderStatus.NEW, 999, 1);
        sale(hidden, OrderStatus.DELIVERED, 999, 1);
        sale(inactive, OrderStatus.DELIVERED, 999, 1);
        var offer = new Promotion();
        offer.setProduct(hidden);
        offer.setUnits(1);
        offer.setPrice(BigDecimal.ONE);
        offer.setHideFromCatalog(true);
        promotions.saveAndFlush(offer);

        var filter = new ProductFilterDto(null, null, null, null, null, false, category.getId());
        var sort = Sort.by(Sort.Direction.DESC, "sold");
        var first = catalog.filterActiveProducts(filter, PageRequest.of(0, 9, sort));
        var second = catalog.filterActiveProducts(filter, PageRequest.of(1, 9, sort));
        assertThat(first.getTotalElements()).isEqualTo(11);
        assertThat(first.getContent()).extracting(ProductViewDto::id)
                .startsWith(pack.getId(), regular.getId(), cancelled.getId(), returned.getId(), pending.getId());
        assertThat(second.getContent()).extracting(ProductViewDto::id)
                .containsExactly(products.findByNameProductIgnoreCaseAndModelIgnoreCase("D unsold 4", "Test").orElseThrow().getId(),
                        products.findByNameProductIgnoreCaseAndModelIgnoreCase("D unsold 5", "Test").orElseThrow().getId());

        pack.setStock(0);
        products.flush();
        var available = new ProductFilterDto(null, null, null, BigDecimal.ONE, BigDecimal.TEN, true, category.getId());
        assertThat(catalog.filterActiveProducts(available, PageRequest.of(0, 9, sort)).getContent())
                .extracting(ProductViewDto::id).startsWith(regular.getId()).doesNotContain(pack.getId());
        var search = new ProductFilterDto("Y regular", null, "Test", null, null, false, category.getId());
        assertThat(catalog.filterActiveProducts(search, PageRequest.of(0, 9, sort)).getContent())
                .extracting(ProductViewDto::id).containsExactly(regular.getId());

        // Changing a delivered order to returned immediately changes the ranking.
        var packOrder = items.findAll().stream().filter(i -> pack.equals(i.getProduct())).findFirst().orElseThrow().getOrder();
        packOrder.setOrderStatus(OrderStatus.RETURNED);
        orders.flush();
        assertThat(catalog.filterActiveProducts(filter, PageRequest.of(0, 9, sort)).getContent())
                .extracting(ProductViewDto::id).startsWith(regular.getId());
    }

    private Product product(String name, User owner, Category category) {
        var product = new Product();
        product.setNameProduct(name);
        product.setModel("Test");
        product.setSku(UUID.randomUUID().toString());
        product.setUrl(UUID.randomUUID().toString());
        product.setCategory(category);
        product.setUser(owner);
        product.setPrice(BigDecimal.TEN);
        product.setStock(100);
        return products.saveAndFlush(product);
    }

    private void sale(Product product, OrderStatus status, int quantity, int units) {
        var order = new OrderEntity();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setCustomerFirstName("Test");
        order.setCustomerLastName("Customer");
        order.setCustomerPhone("0888123456");
        order.setDeliveryType(DeliveryType.ECONT_OFFICE);
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(status);
        order.setSubtotalPrice(BigDecimal.TEN);
        order.setTotalPrice(BigDecimal.TEN);
        orders.saveAndFlush(order);
        var item = new OrderItemEntity();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductName(product.getNameProduct());
        item.setQuantity(quantity);
        item.setUnitsPerItem(units);
        item.setUnitPrice(BigDecimal.ONE);
        item.setTotalPrice(BigDecimal.valueOf(quantity));
        items.saveAndFlush(item);
    }
}
