package nevg.autosilent.Controller;

import nevg.autosilent.Service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {

    private final ProductService productService;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("index");
        modelAndView.addObject("bestSellingProducts", productService.getBestSellingProducts());
        return modelAndView;
    }

    @GetMapping("/cart")
    public ModelAndView cart() {
        return new ModelAndView("cart");
    }
}
