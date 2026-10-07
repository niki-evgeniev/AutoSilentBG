package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Models.Entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductSecondaryCategoryRepositoryTest {

    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private UserRepository users;

    @Test
    void productAppearsInBothCategoryFiltersAndSitemap() {
        String suffix = UUID.randomUUID().toString();
        Category primary = new Category();
        primary.setCategory("Primary-" + suffix);
        primary = categories.saveAndFlush(primary);
        Category secondary = new Category();
        secondary.setCategory("Secondary-" + suffix);
        secondary = categories.saveAndFlush(secondary);

        User owner = new User();
        owner.setEmail("owner-" + suffix + "@example.com");
        owner.setPassword("test-password");
        owner = users.saveAndFlush(owner);

        Product product = new Product();
        product.setNameProduct("Product-" + suffix);
        product.setModel("Model");
        product.setSku("SKU-" + suffix);
        product.setUrl("product-" + suffix);
        product.setCategory(primary);
        product.setSecondaryCategory(secondary);
        product.setPrice(BigDecimal.TEN);
        product.setDescription("Test product");
        product.setStock(1);
        product.setUser(owner);
        product = products.saveAndFlush(product);

        var page = PageRequest.of(0, 10);
        assertThat(products.filterActive(null, null, null, null, null, false, primary.getId(), page))
                .extracting(Product::getId).contains(product.getId());
        assertThat(products.filterActive(null, null, null, null, null, false, secondary.getId(), page))
                .extracting(Product::getId).contains(product.getId());
        assertThat(products.findAllByActiveTrueAndCategoryId(secondary.getId(), page))
                .extracting(Product::getId).contains(product.getId());
        assertThat(products.existsBySecondaryCategoryId(secondary.getId())).isTrue();
        assertThat(categories.findAllWithActiveProductsForSitemap())
                .extracting("id").contains(secondary.getId());
    }
}
