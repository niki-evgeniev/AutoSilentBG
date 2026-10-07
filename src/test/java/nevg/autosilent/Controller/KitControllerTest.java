package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Models.Entity.Kit;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.KitRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Service.KitService;
import nevg.autosilent.Service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KitControllerTest {
    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    @Mock ProductService productService;
    @Mock KitRepository kits;
    @Mock KitService kitService;

    @Test
    void detailsIncludesComponentImageAndFallsBackForMissingImage() {
        Product catalogProduct = new Product();
        catalogProduct.setId(10L);
        catalogProduct.setUrl("front-doors");
        Product insulation = new Product();
        insulation.setId(20L);
        insulation.setUrl("insulation");
        insulation.setActive(true);
        insulation.setStock(5);
        Product adhesive = new Product();
        adhesive.setId(30L);
        adhesive.setUrl("adhesive");
        adhesive.setActive(true);
        adhesive.setStock(5);
        Kit kit = new Kit();
        kit.setCatalogProduct(catalogProduct);
        kit.addItem(insulation, 1);
        kit.addItem(adhesive, 1);

        when(products.findByUrlAndActiveTrueAndCategoryCategoryIgnoreCase("front-doors", "Кит"))
                .thenReturn(Optional.of(catalogProduct));
        when(kits.findByCatalogProductUrlAndCatalogProductActiveTrue("front-doors"))
                .thenReturn(Optional.of(kit));
        when(productService.getActiveProductByUrlAndIncrementCount("front-doors"))
                .thenReturn(Optional.of(details(10L, "front-doors", List.of("/kit.png"))));
        when(productService.getActiveProductByUrl("insulation"))
                .thenReturn(Optional.of(details(20L, "insulation", List.of("/insulation.png"))));
        when(productService.getActiveProductByUrl("adhesive"))
                .thenReturn(Optional.of(details(30L, "adhesive", List.of())));

        var page = new KitController(products, categories, productService, kits, kitService).details("front-doors");

        assertThat(page.getViewName()).isEqualTo("kit-details");
        Map<?, ?> images = (Map<?, ?>) page.getModel().get("kitItemImages");
        assertThat(images.get(20L)).isEqualTo("/insulation.png");
        assertThat(images.containsKey(30L)).isFalse();
    }

    private ProductDetailsDto details(Long id, String url, List<String> images) {
        return new ProductDetailsDto(id, url, "Product", "", "SKU", "Category",
                BigDecimal.ONE, "Description", 5, 0, images);
    }
}
