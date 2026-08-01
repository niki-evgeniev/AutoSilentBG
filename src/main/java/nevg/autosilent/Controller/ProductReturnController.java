package nevg.autosilent.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.ProductReturnRequestDto;
import nevg.autosilent.Service.ClientIpResolver;
import nevg.autosilent.Service.ProductReturnService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;

@Controller
public class ProductReturnController {

    private final ProductReturnService productReturnService;
    private final ClientIpResolver clientIpResolver;

    public ProductReturnController(ProductReturnService productReturnService,
                                   ClientIpResolver clientIpResolver) {
        this.productReturnService = productReturnService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/returns")
    public ModelAndView form() {
        return form(new ProductReturnRequestDto());
    }

    @PostMapping("/returns")
    public ModelAndView submit(@Valid @ModelAttribute("returnRequest") ProductReturnRequestDto returnRequest,
                               BindingResult bindingResult,
                               HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return form(returnRequest);
        }
        productReturnService.create(returnRequest, clientIpResolver.resolve(request));
        return new ModelAndView("redirect:/returns/success");
    }

    @GetMapping("/returns/success")
    public ModelAndView success() {
        return new ModelAndView("return-success");
    }

    private ModelAndView form(ProductReturnRequestDto returnRequest) {
        ModelAndView result = new ModelAndView("product-return");
        result.addObject("returnRequest", returnRequest);
        result.addObject("today", LocalDate.now());
        return result;
    }
}
