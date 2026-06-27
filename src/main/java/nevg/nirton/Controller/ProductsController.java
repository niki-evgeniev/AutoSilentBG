package nevg.nirton.Controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ProductsController {

    @GetMapping("/products")
    public ModelAndView products (){
        return new ModelAndView("products");
    }
}
