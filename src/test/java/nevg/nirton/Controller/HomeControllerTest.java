package nevg.nirton.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    private final HomeController homeController = new HomeController();

    @Test
    void indexReturnsIndexView() {
        ModelAndView result = homeController.index();

        assertThat(result.getViewName()).isEqualTo("index");
        assertThat(result.getModel()).isEmpty();
    }

    @Test
    void cartReturnsCartView() {
        ModelAndView result = homeController.cart();

        assertThat(result.getViewName()).isEqualTo("cart");
        assertThat(result.getModel()).isEmpty();
    }
}
