package nevg.autosilent.Controller;


import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import nevg.autosilent.Models.Dto.CategoryViewDto;
import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Dto.ProductFilterDto;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.Exception.InvalidProductImageException;
import nevg.autosilent.Service.Exception.ProductAlreadyExistsException;
import nevg.autosilent.Service.Exception.ProductCreationException;
import nevg.autosilent.Service.CategoryService;
import nevg.autosilent.Service.ProductService;
import nevg.autosilent.Service.SeoService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Controller
public class ProductsController {

    private final ProductService productService;
    private final SeoService seoService;
    private final CategoryService categoryService;

    public ProductsController(ProductService productService, SeoService seoService, CategoryService categoryService) {
        this.productService = productService;
        this.seoService = seoService;
        this.categoryService = categoryService;
    }

    @GetMapping("/products")
    public ModelAndView products(@RequestParam(name = "search", required = false) String search,
                                 @RequestParam(name = "brand", required = false) String brand,
                                 @RequestParam(name = "model", required = false) String model,
                                 @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
                                 @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
                                 @RequestParam(name = "inStock", defaultValue = "false") boolean inStock,
                                 @RequestParam(name = "order", defaultValue = "default") String order,
                                 @PageableDefault(size = 9, sort = {"nameProduct", "model", "id"},
                                         direction = Sort.Direction.ASC) Pageable pageable) {
        ProductFilterDto filter = new ProductFilterDto(
                search, brand, model, minPrice, maxPrice, inStock, null);
        return catalog(filter, catalogPageable(pageable, order), null, normalizeOrder(order));
    }

    @GetMapping("/products/category/{categoryId}/{slug}")
    public ModelAndView productsByCategory(@PathVariable Long categoryId,
                                           @PathVariable String slug,
                                           @RequestParam(name = "search", required = false) String search,
                                           @RequestParam(name = "brand", required = false) String brand,
                                           @RequestParam(name = "model", required = false) String model,
                                           @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
                                           @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
                                           @RequestParam(name = "inStock", defaultValue = "false") boolean inStock,
                                           @RequestParam(name = "order", defaultValue = "default") String order,
                                           @PageableDefault(size = 9, sort = {"nameProduct", "model", "id"},
                                                   direction = Sort.Direction.ASC) Pageable pageable) {
        var category = categoryService.getById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Категорията не е намерена."));
        if (!category.slug().equals(slug)) {
            ModelAndView redirect = new ModelAndView(
                    "redirect:/products/category/" + category.id() + "/" + category.slug());
            redirect.setStatus(HttpStatus.MOVED_PERMANENTLY);
            return redirect;
        }

        ProductFilterDto filter = new ProductFilterDto(
                search, brand, model, minPrice, maxPrice, inStock, categoryId);
        return catalog(filter, catalogPageable(pageable, order), category, normalizeOrder(order));
    }

    private ModelAndView catalog(ProductFilterDto filter,
                                 Pageable pageable,
                                 CategoryViewDto selectedCategory,
                                 String order) {
        var productPage = productService.filterActiveProducts(filter, pageable);
        ModelAndView modelAndView = new ModelAndView("products");
        modelAndView.addObject("productPage", productPage);
        modelAndView.addObject("products", productPage.getContent());
        modelAndView.addObject("categories", categoryService.getAll());
        modelAndView.addObject("brands", productService.getActiveBrands());
        modelAndView.addObject("models", productService.getActiveModels());
        modelAndView.addObject("filter", filter);
        modelAndView.addObject("search", filter.search() == null ? "" : filter.search());
        modelAndView.addObject("sortMode", order);
        if (selectedCategory != null) {
            modelAndView.addObject("selectedCategory", selectedCategory);
        }
        return modelAndView;
    }

    private Pageable catalogPageable(Pageable pageable, String order) {
        int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int size = pageable == null ? 9 : pageable.getPageSize();
        Sort brandAndModel = Sort.by(Sort.Direction.ASC, "nameProduct", "model", "id");
        Sort sort = switch (normalizeOrder(order)) {
            case "bestSelling" -> Sort.by(Sort.Direction.DESC, "sold").and(brandAndModel);
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price").and(brandAndModel);
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price").and(brandAndModel);
            case "newest" -> Sort.by(Sort.Direction.DESC, "addDate", "id");
            default -> brandAndModel;
        };
        return PageRequest.of(page, size, sort);
    }

    private String normalizeOrder(String order) {
        return switch (order == null ? "" : order) {
            case "bestSelling", "priceAsc", "priceDesc", "newest" -> order;
            default -> "default";
        };
    }

    @GetMapping("/products/{url}")
    public ModelAndView productDetailsPage(@PathVariable String url, HttpServletRequest request) {
        Object csrfAttribute = request.getAttribute(CsrfToken.class.getName());
        if (csrfAttribute instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }
        if (url.matches("\\d+")) {
            Long id = Long.valueOf(url);
            String canonicalUrl = productService.getActiveProductUrl(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Продуктът не е намерен."));
            ModelAndView redirect = new ModelAndView("redirect:/products/" + canonicalUrl);
            redirect.setStatus(HttpStatus.MOVED_PERMANENTLY);
            return redirect;
        }
        Optional<String> currentUrl = productService.getActiveProductUrlByPreviousUrl(url);
        if (currentUrl.isPresent()) {
            ModelAndView redirect = new ModelAndView("redirect:/products/" + currentUrl.get());
            redirect.setStatus(HttpStatus.MOVED_PERMANENTLY);
            return redirect;
        }
        return productDetails(url);
    }

    public ModelAndView productDetailsPage(Long id, HttpServletRequest request) {
        Object csrfAttribute = request.getAttribute(CsrfToken.class.getName());
        if (csrfAttribute instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }
        return productDetails(id);
    }

    public ModelAndView productDetails(String url) {
        ModelAndView modelAndView = new ModelAndView("product-details");
        var product = productService.getActiveProductByUrlAndIncrementCount(url)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Продуктът не е намерен."));
        modelAndView.addObject("product", product);
        seoService.getForProduct(product.id()).ifPresent(seo -> modelAndView.addObject("seo", seo));
        return modelAndView;
    }

    public ModelAndView productDetails(Long id) {
        ModelAndView modelAndView = new ModelAndView("product-details");
        modelAndView.addObject("product", productService.getActiveProductAndIncrementCount(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Продуктът не е намерен.")));
        seoService.getForProduct(id).ifPresent(seo -> modelAndView.addObject("seo", seo));
        return modelAndView;
    }

    @GetMapping("/products/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ModelAndView addProduct() {
        return productForm(new ProductCreateDto());
    }

    @PostMapping("/products/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ModelAndView addProduct(@Valid @ModelAttribute("product") ProductCreateDto product,
                                   BindingResult bindingResult,
                                   @AuthenticationPrincipal ShopUserDetails currentUser,
                                   RedirectAttributes redirectAttributes) {
        if (product.getMainImage() == null || product.getMainImage().isEmpty()) {
            bindingResult.rejectValue("mainImage", "image.required",
                    "Главната снимка е задължителна.");
        }
        long additionalImageCount = product.getAdditionalImages().stream()
                .filter(Objects::nonNull)
                .filter(image -> !image.isEmpty())
                .count();
        if (additionalImageCount > 4) {
            bindingResult.rejectValue("additionalImages", "images.limit",
                    "Можете да добавите най-много 4 допълнителни снимки.");
        }

        if (bindingResult.hasErrors()) {
            return productForm(product);
        }

        try {
            productService.create(product, currentUser.getUsername());
        } catch (ProductAlreadyExistsException exception) {
            rejectDuplicate(bindingResult, exception);
            return productForm(product);
        } catch (InvalidProductImageException exception) {
            bindingResult.reject("images.invalid", exception.getMessage());
            return productForm(product);
        } catch (ProductCreationException exception) {
            bindingResult.reject("product.persistence", exception.getMessage());
            return productForm(product);
        }

        redirectAttributes.addFlashAttribute("productSuccess", true);
        return new ModelAndView("redirect:/products/add");
    }

    @GetMapping("/products/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ModelAndView editProduct(@PathVariable Long id) {
        return editProductForm(id, productService.getForEdit(id));
    }

    @PostMapping("/products/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ModelAndView editProduct(@PathVariable Long id,
                                    @Valid @ModelAttribute("product") ProductCreateDto product,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        long uploadedImageCount = product.getAdditionalImages().stream()
                .filter(Objects::nonNull).filter(image -> !image.isEmpty()).count();
        if (uploadedImageCount > 4) {
            bindingResult.rejectValue("additionalImages", "images.limit",
                    "Можете да добавите най-много 4 допълнителни снимки наведнъж.");
        }
        if (bindingResult.hasErrors()) return editProductForm(id, product);

        try {
            productService.update(id, product);
        } catch (ProductAlreadyExistsException exception) {
            rejectDuplicate(bindingResult, exception);
            return editProductForm(id, product);
        } catch (InvalidProductImageException exception) {
            bindingResult.reject("images.invalid", exception.getMessage());
            return editProductForm(id, product);
        } catch (ProductCreationException exception) {
            bindingResult.reject("product.persistence", exception.getMessage());
            return editProductForm(id, product);
        }

        redirectAttributes.addFlashAttribute("productUpdated", true);
        if (!product.isActive()) {
            return new ModelAndView("redirect:/products");
        }
        String productUrl = productService.getActiveProductUrl(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Продуктът не е намерен."));
        return new ModelAndView("redirect:/products/" + productUrl);
    }

    @PostMapping("/products/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("productDeleted", true);
        return new ModelAndView("redirect:/products");
    }

    private ModelAndView productForm(ProductCreateDto product) {
        ModelAndView modelAndView = new ModelAndView("add-product");
        modelAndView.addObject("product", product);
        modelAndView.addObject("categories", categoryService.getAll());
        modelAndView.addObject("editMode", false);
        return modelAndView;
    }

    private void rejectDuplicate(BindingResult bindingResult, ProductAlreadyExistsException exception) {
        if ("sku".equals(exception.getField())) {
            bindingResult.reject("product.exists", exception.getMessage());
            return;
        }
        bindingResult.rejectValue(exception.getField(), "product.exists", exception.getMessage());
    }

    private ModelAndView editProductForm(Long id, ProductCreateDto product) {
        ProductCreateDto stored = productService.getForEdit(id);
        product.setExistingImages(stored.getExistingImages());
        if (product.getExistingMainImageId() == null) {
            product.setExistingMainImageId(stored.getExistingMainImageId());
        }
        ModelAndView modelAndView = new ModelAndView("add-product");
        modelAndView.addObject("product", product);
        modelAndView.addObject("categories", categoryService.getAll());
        modelAndView.addObject("editMode", true);
        modelAndView.addObject("productId", id);
        return modelAndView;
    }
}
