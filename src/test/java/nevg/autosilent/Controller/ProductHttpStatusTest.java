package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Service.CategoryService;
import nevg.autosilent.Service.ProductService;
import nevg.autosilent.Service.SeoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductHttpStatusTest {

    @Mock
    private ProductService productService;
    @Mock
    private SeoService seoService;
    @Mock
    private CategoryService categoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductsController controller =
                new ProductsController(productService, seoService, categoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void unknownProductReturnsRealHttp404() throws Exception {
        mockMvc.perform(get("/shumoizolaciya/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletedProductReturnsRealHttp404() throws Exception {
        when(productService.getActiveProductUrlByPreviousUrl("deleted-product"))
                .thenReturn(Optional.empty());
        when(productService.getActiveProductByUrlAndIncrementCount("deleted-product"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/shumoizolaciya/deleted-product"))
                .andExpect(status().isNotFound());
    }

    @Test
    void previousProductUrlReturnsPermanentRedirect() throws Exception {
        when(productService.getActiveProductUrlByPreviousUrl("old-product"))
                .thenReturn(Optional.of("current-product"));

        mockMvc.perform(get("/shumoizolaciya/old-product"))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/shumoizolaciya/current-product"));
    }

    @Test
    void temporarilySoldOutProductRemainsHttp200() throws Exception {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "sold-out-product", "Product", "Model", "SKU-1", "Category",
                new BigDecimal("10.00"), "Description", 0, 1L, List.of("/image.png"));
        when(productService.getActiveProductByUrlAndIncrementCount("sold-out-product"))
                .thenReturn(Optional.of(product));

        mockMvc.perform(get("/shumoizolaciya/sold-out-product"))
                .andExpect(status().isOk());
    }

    @Test
    void legacyProductUrlRedirectsPermanentlyToNewPath() throws Exception {
        when(productService.getActiveProductUrlByPreviousUrl("vibrofiltr-2-0"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/products/vibrofiltr-2-0"))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/shumoizolaciya/vibrofiltr-2-0"));
    }

    @Test
    void legacyCatalogUrlWithTrailingSlashRedirectsPermanently() throws Exception {
        mockMvc.perform(get("/products/"))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/shumoizolaciya"));
    }

    @Test
    void catalogReturnsHttp200() throws Exception {
        when(productService.filterActiveProducts(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/shumoizolaciya"))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateRootCategoryReturnsPermanentRedirectToCatalog() throws Exception {
        mockMvc.perform(get("/shumoizolaciya/category/1/zvukoizolatsiya"))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/shumoizolaciya"));

        mockMvc.perform(get("/shumoizolaciya/category/1/zvukoizolatsiya")
                        .param("lang", "en"))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/shumoizolaciya"));
    }
}
