package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.KitComponentDto;
import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Models.Entity.Kit;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.KitRepository;
import nevg.autosilent.Repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KitServiceTest {
    @Mock private KitRepository kits;
    @Mock private ProductRepository products;
    @Mock private ProductService productService;
    @Mock private CategoryRepository categories;
    private KitService service;

    @BeforeEach
    void setUp() {
        service = new KitService(kits, products, productService, categories);
    }

    @Test
    void createStoresThreeSelectedCategoriesOnCatalogProduct() {
        ProductCreateDto request = request();
        request.setKitCategoryIds(List.of(2L, 3L, 4L));
        Category first = category(2L, "Врати");
        Category second = category(3L, "Под");
        Category third = category(4L, "Багажник");
        Product component = new Product();
        component.setId(10L);
        component.setActive(true);
        component.setCategory(category(1L, "Материали"));
        Product catalogProduct = new Product();
        when(categories.findAllById(List.of(2L, 3L, 4L))).thenReturn(List.of(first, second, third));
        when(products.findAllById(java.util.Set.of(10L))).thenReturn(List.of(component));
        when(products.findByNameProductIgnoreCaseAndModelIgnoreCase("Vibrofiltr", "за врати"))
                .thenReturn(Optional.of(catalogProduct));

        service.create(request, "admin@example.com");

        assertThat(catalogProduct.getAdditionalCategories()).containsExactlyInAnyOrder(first, second, third);
        assertThat(request.getSecondaryCategoryId()).isNull();
        verify(products).save(catalogProduct);
        ArgumentCaptor<Kit> savedKit = ArgumentCaptor.forClass(Kit.class);
        verify(kits).save(savedKit.capture());
        assertThat(savedKit.getValue().getCatalogProduct()).isSameAs(catalogProduct);
    }

    @Test
    void createRejectsRepeatedCategoriesBeforeCreatingProduct() {
        ProductCreateDto request = request();
        request.setKitCategoryIds(List.of(2L, 2L, 3L));

        assertThatThrownBy(() -> service.create(request, "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("различни категории");
        verify(productService, never()).create(any(), any());
    }

    @Test
    void createRejectsMoreThanThreeCategories() {
        ProductCreateDto request = request();
        request.setKitCategoryIds(List.of(2L, 3L, 4L, 5L));

        assertThatThrownBy(() -> service.create(request, "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("до три");
        verify(productService, never()).create(any(), any());
    }

    private ProductCreateDto request() {
        ProductCreateDto request = new ProductCreateDto();
        request.setNameProduct("Vibrofiltr");
        request.setModel("за врати");
        KitComponentDto component = new KitComponentDto();
        component.setProductId(10L);
        component.setQuantity(1);
        request.setComponents(List.of(component));
        return request;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setCategory(name);
        return category;
    }
}
