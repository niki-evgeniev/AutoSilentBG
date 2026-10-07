package nevg.autosilent.Controller;

import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Dto.CategoryViewDto;
import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Models.Dto.KitComponentDto;
import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Repository.KitRepository;
import nevg.autosilent.Service.KitService;
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

import java.util.HashMap;
import java.util.Map;

@Controller
public class KitController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductService productService;
    private final KitRepository kits;
    private final KitService kitService;

    public KitController(ProductRepository products, CategoryRepository categories, ProductService productService,
                         KitRepository kits, KitService kitService) {
        this.products = products;
        this.categories = categories;
        this.productService = productService;
        this.kits = kits;
        this.kitService = kitService;
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
        ProductDetailsDto product = productService.getActiveProductByUrlAndIncrementCount(url).orElseThrow();
        var kit = kits.findByCatalogProductUrlAndCatalogProductActiveTrue(url);
        if (kit.isPresent()) {
            product = new ProductDetailsDto(product.id(), product.url(), product.name(), product.model(),
                    product.sku(), product.category(), product.price(), product.description(),
                    kit.get().availableStock(), product.count(), product.imageUrls(), product.secondaryCategory());
        }
        var items = kit.map(value -> value.getItems()).orElseGet(java.util.List::of);
        Map<Long, String> itemImages = new HashMap<>();
        for (var item : items) {
            productService.getActiveProductByUrl(item.getProduct().getUrl())
                    .map(ProductDetailsDto::mainImageUrl)
                    .ifPresent(image -> itemImages.put(item.getProduct().getId(), image));
        }
        return new ModelAndView("kit-details").addObject("product", product)
                .addObject("kitItems", items).addObject("kitItemImages", itemImages);
    }

    @GetMapping("/admin/settings/kits")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView add() {
        ProductCreateDto product = new ProductCreateDto();
        product.setNameProduct("Кит");
        product.setCategoryId(kitCategory().getId());
        product.getComponents().add(new KitComponentDto());
        return form(product);
    }

    @PostMapping("/admin/settings/kits")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView add(@Valid @ModelAttribute("product") ProductCreateDto product, BindingResult errors,
                            @AuthenticationPrincipal ShopUserDetails user, RedirectAttributes redirect) {
        product.setCategoryId(kitCategory().getId());
        if (product.getMainImage() == null || product.getMainImage().isEmpty())
            errors.rejectValue("mainImage", "required", "Добавете основна снимка.");
        if (errors.hasErrors()) return form(product).addObject("imageMustBeReselected",
                product.getMainImage() != null && !product.getMainImage().isEmpty());
        try {
            kitService.create(product, user.getUsername());
        } catch (RuntimeException exception) {
            errors.reject("kit.invalid", exception.getMessage());
            return form(product).addObject("imageMustBeReselected",
                    product.getMainImage() != null && !product.getMainImage().isEmpty());
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
                .addObject("categories", categories.findAllByOrderByCategoryAsc().stream()
                        .map(category -> new CategoryViewDto(category.getId(), category.getCategory())).toList())
                .addObject("availableProducts", productService.getActiveProducts().stream()
                        .filter(item -> !"Кит".equalsIgnoreCase(item.category())).toList());
    }
}
