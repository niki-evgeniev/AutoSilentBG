package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.ProductViewDto;
import nevg.autosilent.Service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private ProductService productService;

    private HomeController homeController;

    @BeforeEach
    void setUp() {
        homeController = new HomeController(productService);
    }

    @Test
    void indexReturnsIndexViewWithBestSellingProducts() {
        List<ProductViewDto> products = List.of(new ProductViewDto(
                1L, "Product", "SKU-1", "Category", new BigDecimal("10.00"),
                "Description", 5, "/image.png"));
        when(productService.getBestSellingProducts()).thenReturn(products);

        ModelAndView result = homeController.index();

        assertThat(result.getViewName()).isEqualTo("index");
        assertThat(result.getModel().get("bestSellingProducts")).isSameAs(products);
    }

    @Test
    void cartReturnsCartView() {
        ModelAndView result = homeController.cart();

        assertThat(result.getViewName()).isEqualTo("cart");
        assertThat(result.getModel()).isEmpty();
    }
}
