package nevg.autosilent.Controller;

import nevg.autosilent.Service.AdminProductInformationService;
import nevg.autosilent.Service.ProductStockService;
import nevg.autosilent.Models.Dto.ProductStockUpdateDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import org.springframework.web.servlet.ModelAndView;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductInformationController {

    private final AdminProductInformationService productInformationService;
    private final ProductStockService stocks;

    public AdminProductInformationController(AdminProductInformationService productInformationService, ProductStockService stocks) {
        this.productInformationService = productInformationService;
        this.stocks = stocks;
    }

    @GetMapping("/admin/settings/information")
    public ModelAndView information(@RequestParam(required = false) Integer updated) {
        ModelAndView modelAndView = new ModelAndView("admin-product-information");
        var information = productInformationService.getProductInformation();
        modelAndView.addObject("products", information.products());
        modelAndView.addObject("summary", information.summary());
        if (updated != null && updated > 0 && updated <= 10000) modelAndView.addObject("stockUpdated", updated);
        return modelAndView;
    }

    @PostMapping("/admin/settings/information/stock")
    @ResponseBody
    public ResponseEntity<?> updateStock(@Valid @RequestBody ProductStockUpdateDto request) {
        try {
            return ResponseEntity.ok(Map.of("updated", stocks.update(request)));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(Map.of("error", exception.getReason()));
        }
    }
}
