package nevg.autosilent.Controller;

import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class KitController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductService productService;

    public KitController(ProductRepository products, CategoryRepository categories, ProductService productService) {
        this.products = products;
        this.categories = categories;
        this.productService = productService;
    }

    @GetMapping("/shumoizolaciya/kit")
    public ModelAndView catalog(@RequestParam(defaultValue = "0") int page) {
        var kits = products.findAllByActiveTrueAndCategoryCategoryIgnoreCase("Кит",
                PageRequest.of(Math.max(page, 0), 9, Sort.by(Sort.Direction.DESC, "addDate", "id")));
        return new ModelAndView("kit").addObject("kits", kits.map(kit ->
                productService.getActiveProductByUrl(kit.getUrl()).orElseThrow()));
    }

    @GetMapping("/shumoizolaciya/kit/{url}")
    public ModelAndView details(@PathVariable String url) {
        products.findByUrlAndActiveTrueAndCategoryCategoryIgnoreCase(url, "Кит")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ModelAndView("kit-details")
                .addObject("product", productService.getActiveProductByUrlAndIncrementCount(url).orElseThrow());
    }

    @GetMapping("/admin/settings/kits")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView add() {
        ProductCreateDto product = new ProductCreateDto();
        product.setCategoryId(kitCategory().getId());
        return form(product);
    }

    @PostMapping("/admin/settings/kits")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView add(@Valid @ModelAttribute("product") ProductCreateDto product, BindingResult errors,
                            @AuthenticationPrincipal ShopUserDetails user, RedirectAttributes redirect) {
        product.setCategoryId(kitCategory().getId());
        if (product.getMainImage() == null || product.getMainImage().isEmpty())
            errors.rejectValue("mainImage", "required", "Добавете основна снимка.");
        if (errors.hasErrors()) return form(product);
        try {
            productService.create(product, user.getUsername());
        } catch (RuntimeException exception) {
            errors.reject("kit.invalid", exception.getMessage());
            return form(product);
        }
        redirect.addFlashAttribute("productSuccess", true);
        return new ModelAndView("redirect:/admin/settings/kits");
    }

    private Category kitCategory() {
        return categories.findByCategoryIgnoreCase("Кит").orElseGet(() -> {
            Category category = new Category();
            category.setCategory("Кит");
            return categories.save(category);
        });
    }

    private ModelAndView form(ProductCreateDto product) {
        return new ModelAndView("add-product").addObject("product", product)
                .addObject("kitMode", true).addObject("editMode", false)
                .addObject("categories", categories.findAllByOrderByCategoryAsc());
    }
}
