package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Models.Dto.ProductSoldQuantityDto;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Repository.OrderItemRepository;
import nevg.autosilent.Repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminProductInformationServiceImplTest {

    private final ProductRepository repository = mock(ProductRepository.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final AdminProductInformationServiceImpl service =
            new AdminProductInformationServiceImpl(repository, orderItemRepository);

    @Test
    void returnsProductRowsAndCalculatedSummary() {
        Product first = product(1L, "Brand A", "Model A", new BigDecimal("10.00"), 4, 12, true);
        Product second = product(2L, "Brand B", "Model B", new BigDecimal("7.50"), 2, 20, false);
        when(repository.findAllByOrderByNameProductAscModelAsc()).thenReturn(List.of(first, second));
        when(orderItemRepository.sumQuantityByProductForStatus(OrderStatus.DELIVERED))
                .thenReturn(List.of(new ProductSoldQuantityDto(1L, 3), new ProductSoldQuantityDto(2L, 5)));

        var result = service.getProductInformation();

        assertThat(result.products()).hasSize(2);
        assertThat(result.products().getFirst().name()).isEqualTo("Brand A Model A");
        assertThat(result.products().getFirst().stockValue()).isEqualByComparingTo("40.00");
        assertThat(result.summary().productCount()).isEqualTo(2);
        assertThat(result.summary().activeProductCount()).isEqualTo(1);
        assertThat(result.summary().totalStock()).isEqualTo(6);
        assertThat(result.summary().totalSold()).isEqualTo(8);
        assertThat(result.summary().totalViews()).isEqualTo(32);
        assertThat(result.summary().totalStockValue()).isEqualByComparingTo("55.00");
    }

    private Product product(Long id, String brand, String model, BigDecimal price, int stock,
                            long views, boolean active) {
        Category category = new Category();
        category.setCategory("Category");
        Product product = new Product();
        product.setId(id);
        product.setNameProduct(brand);
        product.setModel(model);
        product.setSku("SKU-" + model);
        product.setUrl("url-" + model);
        product.setCategory(category);
        product.setPrice(price);
        product.setStock(stock);
        product.setCount(views);
        product.setActive(active);
        return product;
    }
}
