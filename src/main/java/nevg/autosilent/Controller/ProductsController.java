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
import nevg.autosilent.Service.CatalogSeoService;
import nevg.autosilent.Service.ProductService;
import nevg.autosilent.Service.SeoService;
import nevg.autosilent.Utility.CatalogCategoryUrlPolicy;
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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

@Controller
public class ProductsController {

    private final ProductService productService;
    private final SeoService seoService;
    private final CategoryService categoryService;
    private final CatalogSeoService catalogSeoService;

    public ProductsController(ProductService productService, SeoService seoService, CategoryService categoryService,
                              CatalogSeoService catalogSeoService) {
        this.productService = productService;
        this.seoService = seoService;
        this.categoryService = categoryService;
        this.catalogSeoService = catalogSeoService;
    }

    @GetMapping("/shumoizolaciya")
    public ModelAndView products(@RequestParam(name = "search", required = false) String search,
                                 @RequestParam(name = "brand", required = false) String brand,
                                 @RequestParam(name = "model", required = false) String model,
                                 @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
                                 @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
                                 @RequestParam(name = "inStock", defaultValue = "false") boolean inStock,
                                 @RequestParam(name = "order", defaultValue = "default") String order,
                                 @PageableDefault(size = 9, sort = {"nameProduct", "model", "id"},
                                         direction = Sort.Direction.ASC) Pageable pageable,
                                 HttpServletRequest request) {
        ProductFilterDto filter = new ProductFilterDto(
                search, brand, model, minPrice, maxPrice, inStock, null);

        String normalizedOrder = normalizeOrder(order);
        ModelAndView pageZero = normalizeFirstPage(request, "/shumoizolaciya", filter, normalizedOrder);
        return pageZero != null ? pageZero : catalog(filter, catalogPageable(pageable, order),
                null, normalizedOrder);
    }

    @GetMapping({"/products", "/products/"})
    public ModelAndView legacyProducts(HttpServletRequest request) {
        return permanentRedirect("/shumoizolaciya", request.getQueryString());
    }

    @GetMapping("/shumoizolaciya/category/{categoryId}/{slug}")
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
                                                   direction = Sort.Direction.ASC) Pageable pageable,
                                           HttpServletRequest request) {
        if (CatalogCategoryUrlPolicy.isRootCatalogCategory(categoryId)) {
            return permanentRedirect("/shumoizolaciya", null);
        }
        var category = categoryService.getById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Категорията не е намерена."));
        if (!category.slug().equals(slug)) {
            ModelAndView redirect = new ModelAndView(
                    "redirect:/shumoizolaciya/category/" + category.id() + "/" + category.slug());
            redirect.setStatus(HttpStatus.MOVED_PERMANENTLY);
            return redirect;
        }

        ProductFilterDto filter = new ProductFilterDto(
                search, brand, model, minPrice, maxPrice, inStock, categoryId);
        String normalizedOrder = normalizeOrder(order);
        String path = "/shumoizolaciya/category/" + category.id() + "/" + category.slug();
        ModelAndView pageZero = normalizeFirstPage(request, path, filter, normalizedOrder);
        return pageZero != null ? pageZero : catalog(filter, catalogPageable(pageable, order),
                category, normalizedOrder);
    }

    @GetMapping("/products/category/{categoryId}/{slug}")
    public ModelAndView legacyProductsByCategory(@PathVariable Long categoryId,
                                                  @PathVariable String slug,
                                                  HttpServletRequest request) {
        if (CatalogCategoryUrlPolicy.isRootCatalogCategory(categoryId)) {
            return permanentRedirect("/shumoizolaciya", null);
        }
        return permanentRedirect("/shumoizolaciya/category/" + categoryId + "/" + slug,
                request.getQueryString());
    }

    private ModelAndView permanentRedirect(String path, String queryString) {
        String target = queryString == null || queryString.isBlank()
                ? path
                : path + "?" + queryString;
        ModelAndView redirect = new ModelAndView("redirect:" + target);
        redirect.setStatus(HttpStatus.MOVED_PERMANENTLY);
        return redirect;
    }

    private ModelAndView catalog(ProductFilterDto filter,
                                 Pageable pageable,
                                 CategoryViewDto selectedCategory,
                                 String order) {
        var productPage = productService.filterActiveProducts(filter, pageable);
        if (pageable.getPageNumber() > 0 && pageable.getPageNumber() >= productPage.getTotalPages()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Страницата не е намерена.");
        }
        String path = selectedCategory == null ? "/shumoizolaciya"
                : "/shumoizolaciya/category/" + selectedCategory.id() + "/" + selectedCategory.slug();
        var categories = categoryService.getAll();
        Map<Long, String> categoryUrls = new HashMap<>();
        for (CategoryViewDto category : categories) {
            String categoryPath = "/shumoizolaciya/category/" + category.id() + "/" + category.slug();
            categoryUrls.put(category.id(), category.rootCatalogCategory()
                    ? "/shumoizolaciya" : catalogPageUrl(categoryPath, filter, order, 0));
        }
        ModelAndView modelAndView = new ModelAndView("products");
        modelAndView.addObject("productPage", productPage);
        modelAndView.addObject("products", productPage.getContent());
        modelAndView.addObject("categories", categories);
        modelAndView.addObject("categoryUrls", categoryUrls);
        modelAndView.addObject("brands", productService.getActiveBrands());
        modelAndView.addObject("models", productService.getActiveModels());
        modelAndView.addObject("filter", filter);
        modelAndView.addObject("search", filter.search() == null ? "" : filter.search());
        modelAndView.addObject("sortMode", order);
        modelAndView.addObject("paginationLinks", IntStream.range(0, productPage.getTotalPages())
                .mapToObj(page -> catalogPageUrl(path, filter, order, page)).toList());
        if (selectedCategory != null) {
            modelAndView.addObject("selectedCategory", selectedCategory);
        } else {
            modelAndView.addObject("catalogSeo", catalogSeoService.get());
        }
        return modelAndView;
    }

    private ModelAndView normalizeFirstPage(HttpServletRequest request, String path,
                                            ProductFilterDto filter, String order) {
        if (request == null) {
            return null;
        }
        String rawPage = request.getParameter("page");
        int page = 0;
        if (rawPage != null) {
            try {
                page = Integer.parseInt(rawPage);
            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Страницата не е намерена.");
            }
            if (page < 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Страницата не е намерена.");
            }
        }
        if ((rawPage != null && (page == 0 || !rawPage.equals(Integer.toString(page))))
                || request.getParameter("size") != null) {
            ModelAndView redirect = new ModelAndView("redirect:" + catalogPageUrl(path, filter, order, page));
            redirect.setStatus(HttpStatus.MOVED_PERMANENTLY);
            return redirect;
        }
        return null;
    }

    private String catalogPageUrl(String path, ProductFilterDto filter, String order, int page) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath(path);
        if ("en".equals(LocaleContextHolder.getLocale().getLanguage())) {
            url.queryParam("lang", "en");
        }
        if (filter.search() != null) {
            url.queryParam("search", filter.search());
        }
        if (filter.brand() != null) {
            url.queryParam("brand", filter.brand());
        }
        if (filter.model() != null) {
            url.queryParam("model", filter.model());
        }
        if (filter.minPrice() != null) {
            url.queryParam("minPrice", filter.minPrice());
        }
        if (filter.maxPrice() != null) {
            url.queryParam("maxPrice", filter.maxPrice());
        }
        if (filter.inStock()) {
            url.queryParam("inStock", true);
        }
        if (!"default".equals(order)) {
            url.queryParam("order", order);
        }
        if (page > 0) {
            url.queryParam("page", page);
        }
        return url.build().encode().toUriString();
    }

    private Pageable catalogPageable(Pageable pageable, String order) {
        int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int size = 9;
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

    @GetMapping("/shumoizolaciya/{url}")
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
            ModelAndView redirect = new ModelAndView("redirect:/shumoizolaciya/" + canonicalUrl);
            redirect.setStatus(HttpStatus.MOVED_PERMANENTLY);
            return redirect;
        }
        Optional<String> currentUrl = productService.getActiveProductUrlByPreviousUrl(url);
        if (currentUrl.isPresent()) {
            ModelAndView redirect = new ModelAndView("redirect:/shumoizolaciya/" + currentUrl.get());
            redirect.setStatus(HttpStatus.MOVED_PERMANENTLY);
            return redirect;
        }
        return productDetails(url);
    }

    @GetMapping("/products/{url}")
    public ModelAndView legacyProductDetails(@PathVariable String url,
                                              HttpServletRequest request) {
        String targetUrl = url;
        if (url.matches("\\d+")) {
            targetUrl = productService.getActiveProductUrl(Long.valueOf(url))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Продуктът не е намерен."));
        } else {
            targetUrl = productService.getActiveProductUrlByPreviousUrl(url).orElse(url);
        }
        return permanentRedirect("/shumoizolaciya/" + targetUrl, request.getQueryString());
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
        modelAndView.addObject("relatedProducts", productService.getRelatedProducts(product.id()));
        seoService.getForProduct(product.id()).ifPresent(seo -> modelAndView.addObject("seo", seo));
        return modelAndView;
    }

    public ModelAndView productDetails(Long id) {
        ModelAndView modelAndView = new ModelAndView("product-details");
        var product = productService.getActiveProductAndIncrementCount(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Продуктът не е намерен."));
        modelAndView.addObject("product", product);
        modelAndView.addObject("relatedProducts", productService.getRelatedProducts(product.id()));
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
            return new ModelAndView("redirect:/shumoizolaciya");
        }
        String productUrl = productService.getActiveProductUrl(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Продуктът не е намерен."));
        return new ModelAndView("redirect:/shumoizolaciya/" + productUrl);
    }

    @PostMapping("/products/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("productDeleted", true);
        return new ModelAndView("redirect:/shumoizolaciya");
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
