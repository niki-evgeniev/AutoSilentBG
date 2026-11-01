package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.*;
import nevg.autosilent.Models.Entity.*;
import nevg.autosilent.Repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class PromotionIntegrationTest {
    @Autowired PromotionRepository offers;
    @Autowired PromotionService promotions;
    @Autowired ProductRepository products;
    @Autowired ProductService catalog;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired OrderService checkout;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired PromoCodeRepository codes;
    @Autowired OrderItemEditingService editing;
    @Autowired ManualOrderPriceService manualPrices;
    @Autowired WebApplicationContext web;
    Product product;
    Promotion offer;
    User customer;

    @BeforeEach void fixtures() {
        customer = new User(); customer.setEmail(UUID.randomUUID() + "@example.com"); customer.setPassword("test");
        customer.setFirstName("Test"); customer.setLastName("Customer"); customer.setPhoneNumber("0888123456");
        customer.setDiscountPercent(new BigDecimal("20")); users.saveAndFlush(customer);
        var category = new Category(); category.setCategory("Offer " + UUID.randomUUID()); categories.saveAndFlush(category);
        product = new Product(); product.setNameProduct("Promo " + UUID.randomUUID()); product.setSku(UUID.randomUUID().toString());
        product.setUrl(UUID.randomUUID().toString()); product.setPrice(BigDecimal.TEN); product.setStock(100);
        product.setCategory(category); product.setUser(customer); products.saveAndFlush(product);
        offer = new Promotion(); offer.setProduct(product); offer.setUnits(25); offer.setPrice(new BigDecimal("175")); offers.saveAndFlush(offer);
        promotions.initializeSlugs();
        offer = offers.findById(offer.getId()).orElseThrow();
    }

    OrderEntity purchase(List<CartItemOrderDto> cart, String code) {
        String number = checkout.createRegisteredOrder(customer.getEmail(), "Test", "Customer", "0888123456", "", cart, code);
        return orders.findByOrderNumber(number).orElseThrow();
    }

    @Test void twoPacksConsumeFiftyUnitsAndIgnoreCustomerDiscountAndCodes() {
        var order = purchase(List.of(new CartItemOrderDto(product.getId(), 2, offer.getId())), "INVALID-CODE");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("350");
        assertThat(order.getDiscountPrice()).isEqualByComparingTo("0");
        assertThat(order.getPromoCode()).isNull();
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(50);
        var item = items.findByOrderId(order.getId()).getFirst();
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitsPerItem()).isEqualTo(25);
        assertThat(item.getPromotionId()).isEqualTo(offer.getId());
    }

    @Test void mixedOrderDiscountsOnlyOrdinaryItemsAndKeepsSameProductLinesSeparate() {
        var code = new PromoCode(); code.setCode(("P" + UUID.randomUUID().toString().replace("-", "")).toUpperCase(Locale.ROOT));
        code.setCreatedBy(customer); code.setDiscountPercent(10); codes.saveAndFlush(code);
        var order = purchase(List.of(new CartItemOrderDto(product.getId(), 1, offer.getId()), new CartItemOrderDto(product.getId(), 2)), code.getCode());
        assertThat(items.findByOrderId(order.getId())).hasSize(2);
        assertThat(order.getSubtotalPrice()).isEqualByComparingTo("195");
        assertThat(order.getDiscountPrice()).isEqualByComparingTo("2");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("193");
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(73);
        assertThatThrownBy(() -> manualPrices.update(order.getId(), new BigDecimal("174"), customer.getEmail())).isInstanceOf(IllegalArgumentException.class);
        offer.setUnits(1); offer.setPrice(BigDecimal.ONE); offers.saveAndFlush(offer);
        var promoLine = items.findByOrderId(order.getId()).stream().filter(i -> i.getPromotionId() != null).findFirst().orElseThrow();
        editing.remove(order.getId(), promoLine.getId(), customer.getEmail());
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(98);
        assertThat(orders.findById(order.getId()).orElseThrow().getTotalPrice()).isEqualByComparingTo("18");
    }

    @Test void hidingOnlyAffectsCatalogWhileOfferIsLiveAndProductStaysActive() {
        offer.setHideFromCatalog(true); offers.saveAndFlush(offer);
        var filter = new ProductFilterDto(product.getSku(), null, null, null, null, false, null);
        assertThat(catalog.filterActiveProducts(filter, PageRequest.of(0, 9)).getContent()).isEmpty();
        assertThat(products.findById(product.getId()).orElseThrow().isActive()).isTrue();
        offer.setEndsAt(LocalDateTime.now().minusMinutes(1)); offers.saveAndFlush(offer);
        assertThat(catalog.filterActiveProducts(filter, PageRequest.of(0, 9)).getContent()).hasSize(1);
        assertThatThrownBy(() -> purchase(List.of(new CartItemOrderDto(product.getId(), 1, offer.getId())), ""))
                .isInstanceOf(nevg.autosilent.Service.Exception.OrderCreationException.class);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(100);
    }

    @Test void rejectsWrongProductAndInsufficientStockWithoutCreatingOrder() {
        long count = orders.count();
        var other = new Product(); other.setNameProduct("Other"); other.setSku(UUID.randomUUID().toString());
        other.setUrl(UUID.randomUUID().toString()); other.setCategory(product.getCategory()); other.setUser(customer);
        other.setStock(100); other.setPrice(BigDecimal.TEN); products.saveAndFlush(other);
        assertThatThrownBy(() -> purchase(List.of(new CartItemOrderDto(other.getId(), 1, offer.getId())), ""))
                .isInstanceOf(nevg.autosilent.Service.Exception.OrderCreationException.class);
        assertThatThrownBy(() -> purchase(List.of(new CartItemOrderDto(product.getId(), 5, offer.getId())), ""))
                .isInstanceOf(nevg.autosilent.Service.Exception.OrderCreationException.class);
        assertThat(orders.count()).isEqualTo(count);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(100);
    }

    @Test void customerDiscountAndLaterItemEditsKeepPromotionalPriceProtected() {
        var order = purchase(List.of(new CartItemOrderDto(product.getId(), 1, offer.getId()), new CartItemOrderDto(product.getId(), 2)), "");
        assertThat(order.getDiscountPrice()).isEqualByComparingTo("4");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("191");
        editing.add(order.getId(), product.getId(), 1, customer.getEmail());
        assertThat(orders.findById(order.getId()).orElseThrow().getTotalPrice()).isEqualByComparingTo("199");
    }

    @Test void onlyAdminsCanCreateOrEditOffersThroughTheForm() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
                .addFilters(web.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
        for (String role : List.of("MODERATOR", "ADMIN")) {
            var roles = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
            var principal = new nevg.autosilent.Models.Security.ShopUserDetails(customer.getEmail(), "test", "Test", roles);
            var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal, "test", roles);
            var session = new org.springframework.mock.web.MockHttpSession();
            session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new org.springframework.security.core.context.SecurityContextImpl(auth));
            var page = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/admin/settings/promotions").session(session)).andReturn();
            assertThat(page.getResponse().getStatus()).isEqualTo(role.equals("ADMIN") ? 200 : 403);
            var csrf = (org.springframework.security.web.csrf.CsrfToken) page.getRequest()
                    .getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
            long count = offers.count();
            var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/admin/settings/promotions").session(session).param(csrf.getParameterName(), csrf.getToken())
                    .param("productId", product.getId().toString()).param("units", "1").param("price", "8.00")
                    .param("active", "true").param("hideFromCatalog", "true").param("startsAt", "").param("endsAt", "")).andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(role.equals("ADMIN") ? 302 : 403);
            assertThat(offers.count()).isEqualTo(count + (role.equals("ADMIN") ? 1 : 0));
            if (role.equals("ADMIN")) {
                assertThat(result.getFlashMap().get("promotionSaved")).isEqualTo(true);
                var stopped = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/settings/promotions/" + offer.getId()).session(session)
                        .param(csrf.getParameterName(), csrf.getToken()).param("productId", product.getId().toString())
                        .param("units", "25").param("price", "175.00")).andReturn();
                assertThat(stopped.getFlashMap().get("promotionSaved")).isEqualTo(true);
                assertThat(offers.findById(offer.getId()).orElseThrow().isActive()).isFalse();
            }
        }
    }

    @Test void publicPagesRenderSharedCardsAndAdminCanManagePromotions() throws Exception {
        product.setDescription("<p><strong>Formatted description</strong></p><ul><li>Test specification</li></ul><script>unsafe()</script>");
        products.saveAndFlush(product);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).build();
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/promocii")
                .param("search", product.getSku())).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        var page = org.jsoup.Jsoup.parse(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(page.selectFirst("article[data-promotion-id='" + offer.getId() + "']")).isNotNull();
        assertThat(page.selectFirst(".shop-hero-small.page-3d-hero")).isNotNull();
        assertThat(page.select(".custom-breadcrumb li").eachText()).containsExactly("Начало", "Промоции");
        var legacy = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/promocii/" + offer.getId())).andReturn().getResponse();
        assertThat(legacy.getStatus()).isEqualTo(301);
        assertThat(legacy.getRedirectedUrl()).isEqualTo("/promocii/" + product.getUrl());
        var detail = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(legacy.getRedirectedUrl())).andReturn().getResponse();
        assertThat(detail.getStatus()).isEqualTo(200);
        var detailPage = org.jsoup.Jsoup.parse(detail.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(detailPage.selectFirst(".product-description-content strong").text()).isEqualTo("Formatted description");
        assertThat(detailPage.selectFirst(".product-description-content li").text()).isEqualTo("Test specification");
        assertThat(detailPage.select(".product-description-content script")).isEmpty();
        assertThat(detailPage.selectFirst("article[data-promotion-id='" + offer.getId() + "'] .product-add-cart")).isNotNull();
        assertThat(detailPage.selectFirst(".related-products-section")).isNotNull();
        assertThat(detailPage.select(".shop-hero-small")).isEmpty();
        assertThat(detailPage.selectFirst(".product-details-hero .product-gallery")).isNotNull();
        assertThat(detailPage.selectFirst(".product-details-hero .product-details-card")).isNotNull();
        assertThat(detailPage.selectFirst("link[rel=canonical]").attr("href")).endsWith("/promocii/" + product.getUrl());
        assertThat(detailPage.selectFirst("article[data-promotion-id]").attr("data-product-url")).isEqualTo("/promocii/" + product.getUrl());
        assertThat(detailPage.selectFirst(".custom-breadcrumb li:nth-child(2)").text()).isEqualTo("Промоции");
        var roles = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
        var principal = new nevg.autosilent.Models.Security.ShopUserDetails(customer.getEmail(), "test", "Test", roles);
        var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal, "test", roles);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            var admin = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/admin/settings/promotions").principal(auth)).andReturn().getResponse();
            assertThat(admin.getStatus()).isEqualTo(200);
            assertThat(org.jsoup.Jsoup.parse(admin.getContentAsString()).select("input[name=hideFromCatalog]")).isNotEmpty();
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    @Test void relatedProductsExcludeAllLivePromotions() {
        Product ordinary = extraProduct("Ordinary");
        Product promoted = extraProduct("Promoted");
        var extra = new Promotion(); extra.setProduct(promoted); extra.setUnits(1); extra.setPrice(BigDecimal.ONE); offers.saveAndFlush(extra);
        var related = promotions.related(product.getId());
        assertThat(related).extracting(ProductViewDto::id).contains(ordinary.getId()).doesNotContain(product.getId(), promoted.getId());
    }

    Product extraProduct(String name) {
        var extra = new Product(); extra.setNameProduct(name); extra.setSku(UUID.randomUUID().toString());
        extra.setUrl(UUID.randomUUID().toString()); extra.setCategory(product.getCategory()); extra.setUser(customer);
        extra.setStock(10); extra.setPrice(BigDecimal.TEN); return products.saveAndFlush(extra);
    }

    @Test void existingOffersReceiveUniqueStableReadableSlugs() {
        assertThat(offer.getSlug()).isEqualTo(product.getUrl());
        var second = new Promotion(); second.setProduct(product); second.setUnits(25); second.setPrice(BigDecimal.TEN); offers.saveAndFlush(second);
        var third = new Promotion(); third.setProduct(product); third.setUnits(25); third.setPrice(BigDecimal.ONE); offers.saveAndFlush(third);
        promotions.initializeSlugs();
        assertThat(offers.findById(second.getId()).orElseThrow().getSlug()).isEqualTo(product.getUrl() + "-25-br");
        assertThat(offers.findById(third.getId()).orElseThrow().getSlug()).isEqualTo(product.getUrl() + "-25-br-2");
        promotions.initializeSlugs();
        assertThat(promotions.getBySlug(offer.getSlug()).promotionId()).isEqualTo(offer.getId());
    }
}
