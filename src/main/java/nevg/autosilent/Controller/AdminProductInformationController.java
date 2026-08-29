package nevg.autosilent.Controller;

import nevg.autosilent.Service.AdminProductInformationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductInformationController {

    private final AdminProductInformationService productInformationService;

    public AdminProductInformationController(AdminProductInformationService productInformationService) {
        this.productInformationService = productInformationService;
    }

    @GetMapping("/admin/settings/information")
    public ModelAndView information() {
        ModelAndView modelAndView = new ModelAndView("admin-product-information");
        var information = productInformationService.getProductInformation();
        modelAndView.addObject("products", information.products());
        modelAndView.addObject("summary", information.summary());
        return modelAndView;
    }
}
