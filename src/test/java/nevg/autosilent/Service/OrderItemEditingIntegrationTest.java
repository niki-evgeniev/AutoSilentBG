package nevg.autosilent.Service;

import nevg.autosilent.Models.Entity.*;
import nevg.autosilent.Models.Dto.CartItemOrderDto;
import nevg.autosilent.Models.Enums.*;
import nevg.autosilent.Repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class OrderItemEditingIntegrationTest {
    @Autowired OrderItemEditingService editing;
    @Autowired ManualOrderPriceService manualPrices;
    @Autowired OrderService checkout;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired KitRepository kits;
    @Autowired OrderStatusHistoryRepository history;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired org.springframework.web.context.WebApplicationContext web;
    Product first, second;
    OrderEntity order;
    User actor;

    @BeforeEach void prepare() {
        actor = users.findAll().getFirst();
        first = product("20.00"); second = product("10.00");
        String number = checkout.createGuestOrder("test@example.com", "Test", "Customer", "0888123456", "",
                List.of(new CartItemOrderDto(first.getId(), 2)), "");
        order = orders.findByOrderNumber(number).orElseThrow();
        order.setAppliedDiscountPercent(new BigDecimal("10")); order.setDiscountPrice(new BigDecimal("4"));
        order.setTotalPrice(new BigDecimal("36")); orders.saveAndFlush(order);
    }

    @Test void addingAndRemovingUpdatesStockTotalsDiscountAndAudit() {
        editing.add(order.getId(), second.getId(), 3, actor.getEmail());
        var changed = orders.findById(order.getId()).orElseThrow();
        assertThat(changed.getSubtotalPrice()).isEqualByComparingTo("70");
        assertThat(changed.getDiscountPrice()).isEqualByComparingTo("7");
        assertThat(changed.getTotalPrice()).isEqualByComparingTo("63");
        assertThat(stock(second)).isEqualTo(7);
        var added = items.findByOrderId(order.getId()).stream().filter(i -> i.getProductName().equals(second.getDisplayName())).findFirst().orElseThrow();
        editing.remove(order.getId(), added.getId(), actor.getEmail());
        assertThat(stock(second)).isEqualTo(10);
        assertThat(orders.findById(order.getId()).orElseThrow().getTotalPrice()).isEqualByComparingTo("36");
        assertThat(history.findByOrderIdOrderByChangedAtDesc(order.getId())).hasSize(3);
    }

    @Test void removalUsesKitAllocationSnapshotEvenIfKitDefinitionChanges() {
        Product catalog = product("30.00");
        var kit = new Kit(); kit.setCatalogProduct(catalog); kit.addItem(first, 2); kit.addItem(second, 1); kits.saveAndFlush(kit);
        editing.add(order.getId(), catalog.getId(), 2, actor.getEmail());
        assertThat(stock(first)).isEqualTo(4); assertThat(stock(second)).isEqualTo(8);
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var current = kits.findByCatalogProductId(catalog.getId()).orElseThrow();
            current.getItems().getFirst().setQuantity(5);
        });
        var added = items.findByOrderId(order.getId()).stream().filter(i -> i.getProductName().equals(catalog.getDisplayName())).findFirst().orElseThrow();
        editing.remove(order.getId(), added.getId(), actor.getEmail());
        assertThat(stock(first)).isEqualTo(8); assertThat(stock(second)).isEqualTo(10);
    }

    @Test void insufficientKitStockRollsBackEarlierComponentReservations() {
        var catalog = product("30"); var kit = new Kit(); kit.setCatalogProduct(catalog);
        kit.addItem(first, 1); kit.addItem(second, 100); kits.saveAndFlush(kit);
        assertThatThrownBy(() -> editing.add(order.getId(), catalog.getId(), 1, actor.getEmail())).isInstanceOf(IllegalArgumentException.class);
        assertThat(stock(first)).isEqualTo(8); assertThat(stock(second)).isEqualTo(10);
        assertThat(items.findByOrderId(order.getId())).hasSize(1);
    }

    @Test void finalItemAndForeignItemsCannotBeRemovedAndClosedOrdersCannotBeEdited() {
        var original = items.findByOrderId(order.getId()).getFirst();
        assertThatThrownBy(() -> editing.remove(order.getId(), original.getId(), actor.getEmail())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> editing.remove(order.getId(), Long.MAX_VALUE, actor.getEmail())).isInstanceOf(IllegalArgumentException.class);
        order.setOrderStatus(OrderStatus.SHIPPED); orders.saveAndFlush(order);
        assertThatThrownBy(() -> editing.add(order.getId(), second.getId(), 1, actor.getEmail())).isInstanceOf(IllegalArgumentException.class);
        assertThat(stock(first)).isEqualTo(8); assertThat(stock(second)).isEqualTo(10);
    }

    @Test void legacyOrderKeepsItsDiscountAndRestoresProductWithoutAllocationSnapshot() {
        order.setAppliedDiscountPercent(null); orders.saveAndFlush(order);
        var original = items.findByOrderId(order.getId()).getFirst();
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx ->
                items.findById(original.getId()).orElseThrow().getStockAllocation().clear());
        editing.add(order.getId(), second.getId(), 1, actor.getEmail());
        editing.remove(order.getId(), original.getId(), actor.getEmail());
        assertThat(stock(first)).isEqualTo(10);
        assertThat(orders.findById(order.getId()).orElseThrow().getTotalPrice()).isEqualByComparingTo("9");
    }

    @Test void moderatorCanRenderAndUseItemEditor() throws Exception {
        var roles = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MODERATOR"));
        var principal = new nevg.autosilent.Models.Security.ShopUserDetails(actor.getEmail(), "test", "Test", roles);
        var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal, "test", roles);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).build();
            var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/admin/orders/" + order.getId()).principal(auth)).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            var page = org.jsoup.Jsoup.parse(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            assertThat(page.selectFirst("#orderProduct")).isNotNull();
            assertThat(page.select("form[action$='/remove']")).hasSize(1);
            var added = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/admin/orders/" + order.getId() + "/items/add").principal(auth)
                    .param("productId", second.getId().toString()).param("quantity", "2")).andReturn().getResponse();
            assertThat(added.getStatus()).isEqualTo(302);
            assertThat(stock(second)).isEqualTo(8);
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    Product product(String price) {
        var category = new Category(); category.setCategory("Edit " + UUID.randomUUID()); categories.save(category);
        var product = new Product(); product.setNameProduct("Edit " + UUID.randomUUID());
        product.setSku(UUID.randomUUID().toString()); product.setUrl(UUID.randomUUID().toString());
        product.setCategory(category); product.setUser(actor); product.setStock(10); product.setPrice(new BigDecimal(price));
        return products.saveAndFlush(product);
    }
    int stock(Product product) { return products.findById(product.getId()).orElseThrow().getStock(); }

    @Test void manualPriceCalculatesDiscountAndReplacesPromoWithoutChangingStock() {
        order.setPromoCode("OLD-PROMO"); order.setPromoDiscountPercent(new BigDecimal("10")); orders.saveAndFlush(order);
        manualPrices.update(order.getId(), new BigDecimal("33.00"), actor.getEmail());
        var changed = orders.findById(order.getId()).orElseThrow();
        assertThat(changed.getTotalPrice()).isEqualByComparingTo("33");
        assertThat(changed.getDiscountPrice()).isEqualByComparingTo("7");
        assertThat(changed.getAppliedDiscountPercent()).isEqualByComparingTo("17.5");
        assertThat(changed.getPromoCode()).isNull();
        assertThat(changed.getPromoDiscountPercent()).isEqualByComparingTo("0");
        assertThat(stock(first)).isEqualTo(8);
        assertThat(history.findByOrderIdOrderByChangedAtDesc(order.getId())).hasSize(2);
    }

    @Test void invalidOrPaidOrderPriceLeavesTheOrderUnchanged() {
        for (String value : List.of("-1", "41", "0.001")) {
            assertThatThrownBy(() -> manualPrices.update(order.getId(), new BigDecimal(value), actor.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(orders.findById(order.getId()).orElseThrow().getTotalPrice()).isEqualByComparingTo("36");
        order.setPaymentStatus(PaymentStatus.PAID); orders.saveAndFlush(order);
        assertThatThrownBy(() -> manualPrices.update(order.getId(), new BigDecimal("33"), actor.getEmail()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(history.findByOrderIdOrderByChangedAtDesc(order.getId())).hasSize(1);
    }

    @Test void manualPriceIsVisibleAndAccessibleOnlyToAdminWithCsrf() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
                .addFilters(web.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
        for (String role : List.of("MODERATOR", "ADMIN")) {
            var roles = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
            var principal = new nevg.autosilent.Models.Security.ShopUserDetails(actor.getEmail(), "test", "Test", roles);
            var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal, "test", roles);
            var session = new org.springframework.mock.web.MockHttpSession();
            session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new org.springframework.security.core.context.SecurityContextImpl(auth));
            var pageResult = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/admin/orders/" + order.getId()).session(session)).andReturn();
            assertThat(pageResult.getResponse().getStatus()).isEqualTo(200);
            var page = org.jsoup.Jsoup.parse(pageResult.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            assertThat(page.select("#manualOrderTotal")).hasSize(role.equals("ADMIN") ? 1 : 0);
            var csrf = (org.springframework.security.web.csrf.CsrfToken) pageResult.getRequest()
                    .getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
            var denied = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/admin/orders/" + order.getId() + "/price").session(session).param("total", "33.00")).andReturn();
            assertThat(denied.getResponse().getStatus()).isEqualTo(403);
            var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/admin/orders/" + order.getId() + "/price").session(session).param("total", "33.00")
                    .param(csrf.getParameterName(), csrf.getToken())).andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(role.equals("ADMIN") ? 302 : 403);
            assertThat(orders.findById(order.getId()).orElseThrow().getTotalPrice())
                    .isEqualByComparingTo(role.equals("ADMIN") ? "33" : "36");
        }
    }
}
