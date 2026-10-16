package nevg.autosilent.Controller;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Models.Entity.Kit;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.KitRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.SitemapService;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
class KitLocalizationRenderTest {
    @Autowired private WebApplicationContext context;
    @Autowired private MessageSource messages;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private UserRepository users;
    @Autowired private KitRepository kits;
    @Autowired private SitemapService sitemap;

    @Test
    void kitCatalogRendersBulgarianAndEnglishInterface() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();

        var bulgarian = Jsoup.parse(mvc.perform(get("/shumoizolaciya/kit"))
                .andReturn().getResponse().getContentAsString());
        assertThat(bulgarian.title()).isEqualTo("Китове - AutoSilent");
        assertThat(bulgarian.selectFirst(".section-title h1").text()).isEqualTo("Китове");
        assertThat(bulgarian.selectFirst(".section-title p").text())
                .isEqualTo("Готови комплекти от продукти за шумоизолация.");
        assertThat(bulgarian.selectFirst("link[rel=canonical]").attr("href"))
                .isEqualTo("https://autosilent.bg/shumoizolaciya/kit");
        assertThat(bulgarian.selectFirst("meta[name=robots]").attr("content"))
                .doesNotContain("noindex");

        assertThat(messages.getMessage("kit.catalog.intro", null, Locale.ENGLISH))
                .isEqualTo("Ready-made soundproofing product kits.");

        var english = Jsoup.parse(mvc.perform(get("/shumoizolaciya/kit").param("lang", "en"))
                .andReturn().getResponse().getContentAsString());
        assertThat(english.title()).isEqualTo("Kits - AutoSilent");
        assertThat(english.selectFirst(".section-title h1").text()).isEqualTo("Kits");
        assertThat(english.selectFirst(".section-title p").text())
                .isEqualTo("Ready-made soundproofing product kits.");
        assertThat(english.selectFirst("link[rel=canonical]").attr("href"))
                .isEqualTo("https://autosilent.bg/shumoizolaciya/kit");
    }

    @Test
    @Transactional
    void kitDetailsTranslateInterfaceButKeepStoredContent() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Category kitCategory = categories.findByCategoryIgnoreCase("Кит").orElseGet(() -> {
            Category category = new Category();
            category.setCategory("Кит");
            return categories.saveAndFlush(category);
        });
        Category materialCategory = new Category();
        materialCategory.setCategory("Материал-" + suffix);
        materialCategory = categories.saveAndFlush(materialCategory);
        User owner = new User();
        owner.setEmail("kit-locale-" + suffix + "@example.com");
        owner.setPassword("test-password");
        owner = users.saveAndFlush(owner);

        Product catalogProduct = product("Кит-" + suffix, "за врати", kitCategory, owner, suffix);
        Product component = product("Материал", "2 mm", materialCategory, owner, suffix + "-component");
        Kit kit = new Kit();
        kit.setCatalogProduct(catalogProduct);
        kit.addItem(component, 2);
        kits.saveAndFlush(kit);

        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String path = "/shumoizolaciya/kit/" + catalogProduct.getUrl();
        var bulgarian = Jsoup.parse(mvc.perform(get(path)).andReturn().getResponse().getContentAsString());
        var english = Jsoup.parse(mvc.perform(get(path).param("lang", "en"))
                .andReturn().getResponse().getContentAsString());

        assertThat(bulgarian.selectFirst(".product-details-price").text()).isEqualTo("Цена: 19.90 €");
        assertThat(bulgarian.selectFirst(".quantity-control span").text()).isEqualTo("Количество");
        assertThat(bulgarian.select(".product-information-card h2"))
                .extracting(element -> element.text()).contains("Описание", "Продукти в кита");
        assertThat(bulgarian.selectFirst(".list-group-item > span").text()).isEqualTo("2 бр.");

        assertThat(english.selectFirst(".product-details-price").text()).isEqualTo("Price: 19.90 €");
        assertThat(english.selectFirst(".quantity-control span").text()).isEqualTo("Quantity");
        assertThat(english.select(".product-information-card h2"))
                .extracting(element -> element.text()).contains("Description", "Products in the kit");
        assertThat(english.selectFirst(".list-group-item > span").text()).isEqualTo("2 pcs");
        assertThat(english.selectFirst(".product-details-card h1").text())
                .isEqualTo(bulgarian.selectFirst(".product-details-card h1").text());
        assertThat(english.selectFirst(".product-description-content").text())
                .isEqualTo(bulgarian.selectFirst(".product-description-content").text());
        assertThat(bulgarian.selectFirst("link[rel=canonical]").attr("href"))
                .isEqualTo("https://autosilent.bg" + path);
        assertThat(english.selectFirst("link[rel=canonical]").attr("href"))
                .isEqualTo("https://autosilent.bg" + path);
        assertThat(sitemap.generateSitemap())
                .contains("<loc>https://autosilent.bg" + path + "</loc>")
                .doesNotContain("<loc>https://autosilent.bg/shumoizolaciya/" + catalogProduct.getUrl() + "</loc>");

        var englishCatalog = Jsoup.parse(mvc.perform(get("/shumoizolaciya/kit").param("lang", "en"))
                .andReturn().getResponse().getContentAsString());
        assertThat(englishCatalog.selectFirst(".product-card .product-meta span").text()).isEqualTo("Kit");
        assertThat(englishCatalog.selectFirst(".product-card .price").text()).isEqualTo("Price: 19.90 €");
        assertThat(englishCatalog.selectFirst(".product-card .btn").text()).isEqualTo("View more");
        assertThat(englishCatalog.selectFirst(".product-card h3").text())
                .isEqualTo(catalogProduct.getDisplayName());
    }

    private Product product(String brand, String model, Category category, User owner, String suffix) {
        Product product = new Product();
        product.setNameProduct(brand);
        product.setModel(model);
        product.setSku("SKU-" + suffix);
        product.setUrl("kit-locale-" + suffix);
        product.setCategory(category);
        product.setPrice(new BigDecimal("19.90"));
        product.setDescription("Описание от базата данни");
        product.setStock(10);
        product.setUser(owner);
        return products.saveAndFlush(product);
    }
}
