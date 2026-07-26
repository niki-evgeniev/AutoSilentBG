package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Dto.CategoryViewDto;
import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Models.Dto.ProductFilterDto;
import nevg.autosilent.Models.Dto.ProductViewDto;
import nevg.autosilent.Models.Dto.ProductImageEditDto;
import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.Exception.InvalidProductImageException;
import nevg.autosilent.Service.Exception.ProductAlreadyExistsException;
import nevg.autosilent.Service.Exception.ProductCreationException;
import nevg.autosilent.Service.ProductService;
import nevg.autosilent.Service.CategoryService;
import nevg.autosilent.Service.SeoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductsControllerTest {

    @Mock
    private ProductService productService;
    @Mock
    private SeoService seoService;
    @Mock
    private CategoryService categoryService;

    private ProductsController productsController;

    @BeforeEach
    void setUp() {
        productsController = new ProductsController(productService, seoService, categoryService);
    }

    @Test
    void productsReturnsActiveProductsInModel() {
        List<ProductViewDto> products = List.of(new ProductViewDto(
                1L, "Product", "SKU-1", "Category", new BigDecimal("10.00"),
                "Description", 2, "/image.png"
        ));
        PageRequest pageable = PageRequest.of(0, 9);
        ProductFilterDto filter = new ProductFilterDto(null, null, null, null, null, false, null);
        when(productService.filterActiveProducts(filter, pageable)).thenReturn(new PageImpl<>(products));

        ModelAndView result = productsController.products(
                null, null, null, null, null, false, pageable);

        assertThat(result.getViewName()).isEqualTo("products");
        assertThat(result.getModel().get("products")).isEqualTo(products);
        assertThat(result.getModel().get("productPage")).isNotNull();
        assertThat(result.getModel().get("filter")).isEqualTo(filter);
    }

    @Test
    void productsReturnsEmptyCatalogInModel() {
        PageRequest pageable = PageRequest.of(0, 9);
        ProductFilterDto filter = new ProductFilterDto(null, null, null, null, null, false, null);
        when(productService.filterActiveProducts(filter, pageable)).thenReturn(new PageImpl<>(List.of()));

        ModelAndView result = productsController.products(
                null, null, null, null, null, false, pageable);

        assertThat(result.getViewName()).isEqualTo("products");
        assertThat(result.getModel().get("products")).isEqualTo(List.of());
    }

    @Test
    void productsSearchesAndPreservesTrimmedSearchTerm() {
        PageRequest pageable = PageRequest.of(2, 9);
        ProductFilterDto filter = new ProductFilterDto(
                "  lamp  ", "  Brand  ", " Model ", new BigDecimal("10"), new BigDecimal("50"), true, null);
        when(productService.filterActiveProducts(filter, pageable)).thenReturn(new PageImpl<>(List.of()));

        ModelAndView result = productsController.products(
                "  lamp  ", "  Brand  ", " Model ",
                new BigDecimal("10"), new BigDecimal("50"), true, pageable);

        assertThat(result.getModel().get("search")).isEqualTo("lamp");
        assertThat(result.getModel().get("filter")).isEqualTo(filter);
        verify(productService).filterActiveProducts(filter, pageable);
    }

    @Test
    void categoryPageShowsOnlyProductsFromCanonicalCategory() {
        CategoryViewDto category = new CategoryViewDto(3L, "Звукоизолация");
        PageRequest pageable = PageRequest.of(0, 9);
        PageImpl<ProductViewDto> page = new PageImpl<>(List.of());
        ProductFilterDto filter = new ProductFilterDto(
                null, "Brand", "Model", null, new BigDecimal("100"), true, 3L);
        when(categoryService.getById(3L)).thenReturn(Optional.of(category));
        when(productService.filterActiveProducts(filter, pageable)).thenReturn(page);

        ModelAndView result = productsController.productsByCategory(
                3L, "zvukoizolatsiya", null, "Brand", "Model",
                null, new BigDecimal("100"), true, pageable);

        assertThat(result.getViewName()).isEqualTo("products");
        assertThat(result.getModel().get("selectedCategory")).isSameAs(category);
        assertThat(result.getModel().get("productPage")).isSameAs(page);
        assertThat(result.getModel().get("filter")).isEqualTo(filter);
    }

    @Test
    void categoryPageRedirectsNonCanonicalSlugPermanently() {
        CategoryViewDto category = new CategoryViewDto(3L, "Звукоизолация");
        when(categoryService.getById(3L)).thenReturn(Optional.of(category));

        ModelAndView result = productsController.productsByCategory(
                3L, "wrong", null, null, null,
                null, null, false, PageRequest.of(0, 9));

        assertThat(result.getViewName())
                .isEqualTo("redirect:/products/category/3/zvukoizolatsiya");
        assertThat(result.getStatus()).isEqualTo(HttpStatus.MOVED_PERMANENTLY);
        verify(productService, never()).filterActiveProducts(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void productDetailsReturnsRequestedProduct() {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "Product", "SKU-1", "Category", new BigDecimal("10.00"),
                "Description", 2, 1L, List.of("/image.png")
        );
        when(productService.getActiveProductAndIncrementCount(7L)).thenReturn(Optional.of(product));

        ModelAndView result = productsController.productDetails(7L);

        assertThat(result.getViewName()).isEqualTo("product-details");
        assertThat(result.getModel().get("product")).isSameAs(product);
    }

    @Test
    void productDetailsByUrlReturnsCanonicalProductPage() {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "product-model", "Product", "Model", "SKU-1", "Category",
                new BigDecimal("10.00"), "Description", 2, 1L, List.of("/image.png")
        );
        when(productService.getActiveProductByUrlAndIncrementCount("product-model"))
                .thenReturn(Optional.of(product));

        ModelAndView result = productsController.productDetails("product-model");

        assertThat(result.getViewName()).isEqualTo("product-details");
        assertThat(result.getModel().get("product")).isSameAs(product);
        verify(seoService).getForProduct(7L);
    }

    @Test
    void numericProductUrlRedirectsToCanonicalUrl() {
        when(productService.getActiveProductUrl(7L)).thenReturn(Optional.of("product-model"));

        ModelAndView result = productsController.productDetailsPage(
                "7", new MockHttpServletRequest());

        assertThat(result.getViewName()).isEqualTo("redirect:/products/product-model");
        assertThat(result.getStatus()).isEqualTo(HttpStatus.MOVED_PERMANENTLY);
    }

    @Test
    void previousProductUrlRedirectsPermanentlyToCurrentUrl() {
        when(productService.getActiveProductUrlByPreviousUrl("old-product-name"))
                .thenReturn(Optional.of("new-product-name"));

        ModelAndView result = productsController.productDetailsPage(
                "old-product-name", new MockHttpServletRequest());

        assertThat(result.getViewName()).isEqualTo("redirect:/products/new-product-name");
        assertThat(result.getStatus()).isEqualTo(HttpStatus.MOVED_PERMANENTLY);
        verify(productService, never())
                .getActiveProductByUrlAndIncrementCount("old-product-name");
    }

    @Test
    void soldOutActiveProductStillReturnsProductPage() {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "sold-out-product", "Product", "Model", "SKU-1", "Category",
                new BigDecimal("10.00"), "Description", 0, 1L, List.of("/image.png")
        );
        when(productService.getActiveProductByUrlAndIncrementCount("sold-out-product"))
                .thenReturn(Optional.of(product));

        ModelAndView result = productsController.productDetailsPage(
                "sold-out-product", new MockHttpServletRequest());

        assertThat(result.getViewName()).isEqualTo("product-details");
        assertThat(((ProductDetailsDto) result.getModel().get("product")).stock()).isZero();
    }

    @Test
    void unknownOrDeletedProductUrlReturnsNotFound() {
        when(productService.getActiveProductByUrlAndIncrementCount("missing-product"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productsController.productDetailsPage(
                "missing-product", new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void productDetailsAddsSeoWhenConfigured() {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "Product", "SKU-1", "Category", new BigDecimal("10.00"),
                "Description", 2, 1L, List.of("/image.png")
        );
        SeoDto seo = new SeoDto();
        seo.setTitle("Search title");
        when(productService.getActiveProductAndIncrementCount(7L)).thenReturn(Optional.of(product));
        when(seoService.getForProduct(7L)).thenReturn(Optional.of(seo));

        ModelAndView result = productsController.productDetails(7L);

        assertThat(result.getModel().get("seo")).isSameAs(seo);
    }

    @Test
    void productDetailsPageInitializesCsrfTokenBeforeRenderingQuickOrderForm() {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "Product", "SKU-1", "Category", new BigDecimal("10.00"),
                "Description", 2, 1L, List.of("/image.png")
        );
        when(productService.getActiveProductAndIncrementCount(7L)).thenReturn(Optional.of(product));
        CsrfToken csrfToken = org.mockito.Mockito.mock(CsrfToken.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CsrfToken.class.getName(), csrfToken);

        ModelAndView result = productsController.productDetailsPage(7L, request);

        verify(csrfToken).getToken();
        assertThat(result.getViewName()).isEqualTo("product-details");
        assertThat(result.getModel().get("product")).isSameAs(product);
    }

    @Test
    void productDetailsPageWorksWhenCsrfAttributeIsNotPresent() {
        ProductDetailsDto product = new ProductDetailsDto(
                8L, "Product", "SKU-2", "Category", new BigDecimal("12.00"),
                "Description", 1, 1L, List.of()
        );
        when(productService.getActiveProductAndIncrementCount(8L)).thenReturn(Optional.of(product));

        ModelAndView result = productsController.productDetailsPage(8L, new MockHttpServletRequest());

        assertThat(result.getViewName()).isEqualTo("product-details");
        assertThat(result.getModel().get("product")).isSameAs(product);
    }

    @Test
    void productDetailsPageReturnsNotFoundForMissingProduct() {
        when(productService.getActiveProductAndIncrementCount(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productsController.productDetailsPage(
                404L, new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void productDetailsReturnsNotFoundForMissingOrInactiveProduct() {
        when(productService.getActiveProductAndIncrementCount(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productsController.productDetails(404L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void addProductFormReturnsEmptyProductModel() {
        ModelAndView result = productsController.addProduct();

        assertThat(result.getViewName()).isEqualTo("add-product");
        assertThat(result.getModel().get("product")).isInstanceOf(ProductCreateDto.class);
    }

    @Test
    void addProductRejectsMissingMainImage() {
        ProductCreateDto product = validProduct();
        product.setMainImage(null);
        BindingResult bindingResult = bindingResult(product);

        ModelAndView result = productsController.addProduct(
                product, bindingResult, currentUser(), new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("add-product");
        assertThat(result.getModel().get("product")).isSameAs(product);
        assertThat(bindingResult.getFieldError("mainImage"))
                .isNotNull()
                .extracting(error -> error.getCode())
                .isEqualTo("image.required");
        verify(productService, never()).create(product, "owner@example.com");
    }

    @Test
    void addProductRejectsMoreThanFourAdditionalImages() {
        ProductCreateDto product = validProduct();
        product.setAdditionalImages(List.of(image(), image(), image(), image(), image()));
        BindingResult bindingResult = bindingResult(product);

        ModelAndView result = productsController.addProduct(
                product, bindingResult, currentUser(), new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("add-product");
        assertThat(bindingResult.getFieldError("additionalImages"))
                .isNotNull()
                .extracting(error -> error.getCode())
                .isEqualTo("images.limit");
        verify(productService, never()).create(product, "owner@example.com");
    }

    @Test
    void addProductAddsFieldErrorForDuplicateProduct() {
        ProductCreateDto product = validProduct();
        BindingResult bindingResult = bindingResult(product);
        doThrow(new ProductAlreadyExistsException("sku", "Duplicate SKU"))
                .when(productService).create(product, "owner@example.com");

        ModelAndView result = productsController.addProduct(
                product, bindingResult, currentUser(), new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("add-product");
        assertThat(bindingResult.getGlobalError())
                .isNotNull()
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo("product.exists");
                    assertThat(error.getDefaultMessage()).isEqualTo("Duplicate SKU");
                });
    }

    @Test
    void addProductAddsGlobalErrorForInvalidImage() {
        ProductCreateDto product = validProduct();
        BindingResult bindingResult = bindingResult(product);
        doThrow(new InvalidProductImageException("Invalid image"))
                .when(productService).create(product, "owner@example.com");

        ModelAndView result = productsController.addProduct(
                product, bindingResult, currentUser(), new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("add-product");
        assertThat(bindingResult.getGlobalError())
                .isNotNull()
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo("images.invalid");
                    assertThat(error.getDefaultMessage()).isEqualTo("Invalid image");
                });
    }

    @Test
    void addProductAddsGlobalErrorForPersistenceFailure() {
        ProductCreateDto product = validProduct();
        BindingResult bindingResult = bindingResult(product);
        doThrow(new ProductCreationException("Persistence failure", new RuntimeException()))
                .when(productService).create(product, "owner@example.com");

        ModelAndView result = productsController.addProduct(
                product, bindingResult, currentUser(), new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("add-product");
        assertThat(bindingResult.getGlobalError())
                .isNotNull()
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo("product.persistence");
                    assertThat(error.getDefaultMessage()).isEqualTo("Persistence failure");
                });
    }

    @Test
    void addProductCreatesProductAndRedirectsOnSuccess() {
        ProductCreateDto product = validProduct();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView result = productsController.addProduct(
                product, bindingResult(product), currentUser(), redirectAttributes);

        verify(productService).create(product, "owner@example.com");
        assertThat(result.getViewName()).isEqualTo("redirect:/products/add");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("productSuccess");
    }

    @Test
    void deleteProductDeactivatesProductAndRedirectsToCatalog() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView result = productsController.deleteProduct(7L, redirectAttributes);

        verify(productService).delete(7L);
        assertThat(result.getViewName()).isEqualTo("redirect:/products");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("productDeleted");
    }

    @Test
    void editProductReturnsPopulatedEditForm() {
        ProductCreateDto stored = editableProduct(true);
        stored.setExistingMainImageId(11L);
        stored.setExistingImages(List.of(new ProductImageEditDto(11L, "/main.jpg", true)));
        when(productService.getForEdit(9L)).thenReturn(stored);

        ModelAndView result = productsController.editProduct(9L);

        assertThat(result.getViewName()).isEqualTo("add-product");
        assertThat(result.getModel().get("product")).isSameAs(stored);
        assertThat(result.getModel().get("editMode")).isEqualTo(true);
        assertThat(result.getModel().get("productId")).isEqualTo(9L);
    }

    @Test
    void editProductRejectsMoreThanFourUploadedImages() {
        ProductCreateDto submitted = editableProduct(true);
        submitted.setAdditionalImages(List.of(image(), image(), image(), image(), image()));
        ProductCreateDto stored = editableProduct(true);
        when(productService.getForEdit(9L)).thenReturn(stored);
        BindingResult binding = bindingResult(submitted);

        ModelAndView result = productsController.editProduct(
                9L, submitted, binding, new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("add-product");
        assertThat(binding.getFieldError("additionalImages")).isNotNull();
        verify(productService, never()).update(9L, submitted);
    }

    @Test
    void editProductUpdatesActiveProductAndRedirectsToDetails() {
        ProductCreateDto submitted = editableProduct(true);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(productService.getActiveProductUrl(9L)).thenReturn(Optional.of("product-model"));

        ModelAndView result = productsController.editProduct(
                9L, submitted, bindingResult(submitted), redirect);

        verify(productService).update(9L, submitted);
        assertThat(result.getViewName()).isEqualTo("redirect:/products/product-model");
        assertThat(redirect.getFlashAttributes().get("productUpdated")).isEqualTo(true);
    }

    @Test
    void editProductUpdatesInactiveProductAndRedirectsToCatalog() {
        ProductCreateDto submitted = editableProduct(false);

        ModelAndView result = productsController.editProduct(
                9L, submitted, bindingResult(submitted), new RedirectAttributesModelMap());

        verify(productService).update(9L, submitted);
        assertThat(result.getViewName()).isEqualTo("redirect:/products");
    }

    @Test
    void editProductMapsDuplicateToFieldError() {
        ProductCreateDto submitted = editableProduct(true);
        BindingResult binding = bindingResult(submitted);
        ProductCreateDto stored = editableProduct(true);
        when(productService.getForEdit(9L)).thenReturn(stored);
        doThrow(new ProductAlreadyExistsException("sku", "Duplicate SKU"))
                .when(productService).update(9L, submitted);

        ModelAndView result = productsController.editProduct(
                9L, submitted, binding, new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("add-product");
        assertThat(binding.getGlobalError()).isNotNull()
                .satisfies(error -> assertThat(error.getDefaultMessage()).isEqualTo("Duplicate SKU"));
    }

    private ProductCreateDto validProduct() {
        ProductCreateDto product = new ProductCreateDto();
        product.setMainImage(image());
        return product;
    }

    private ProductCreateDto editableProduct(boolean active) {
        ProductCreateDto product = new ProductCreateDto();
        product.setActive(active);
        return product;
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "image.png", "image/png", new byte[]{1});
    }

    private ShopUserDetails currentUser() {
        return new ShopUserDetails("owner@example.com", "password", "Ivan", List.of());
    }

    private BindingResult bindingResult(ProductCreateDto product) {
        return new BeanPropertyBindingResult(product, "product");
    }
}
