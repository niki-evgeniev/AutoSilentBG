package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Models.Dto.ProductViewDto;
import nevg.autosilent.Models.Dto.ProductImageEditDto;
import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.Exception.InvalidProductImageException;
import nevg.autosilent.Service.Exception.ProductAlreadyExistsException;
import nevg.autosilent.Service.Exception.ProductCreationException;
import nevg.autosilent.Service.ProductService;
import nevg.autosilent.Service.SeoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    private ProductsController productsController;

    @BeforeEach
    void setUp() {
        productsController = new ProductsController(productService, seoService);
    }

    @Test
    void productsReturnsActiveProductsInModel() {
        List<ProductViewDto> products = List.of(new ProductViewDto(
                1L, "Product", "SKU-1", "Category", new BigDecimal("10.00"),
                "Description", 2, "/image.png"
        ));
        when(productService.searchActiveProducts(null)).thenReturn(products);

        ModelAndView result = productsController.products(null);

        assertThat(result.getViewName()).isEqualTo("products");
        assertThat(result.getModel().get("products")).isSameAs(products);
    }

    @Test
    void productsReturnsEmptyCatalogInModel() {
        when(productService.searchActiveProducts(null)).thenReturn(List.of());

        ModelAndView result = productsController.products(null);

        assertThat(result.getViewName()).isEqualTo("products");
        assertThat(result.getModel().get("products")).isEqualTo(List.of());
    }

    @Test
    void productsSearchesAndPreservesTrimmedSearchTerm() {
        when(productService.searchActiveProducts("  lamp  ")).thenReturn(List.of());

        ModelAndView result = productsController.products("  lamp  ");

        assertThat(result.getModel().get("search")).isEqualTo("lamp");
        verify(productService).searchActiveProducts("  lamp  ");
    }

    @Test
    void productDetailsReturnsRequestedProduct() {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "Product", "SKU-1", "Category", new BigDecimal("10.00"),
                "Description", 2, List.of("/image.png")
        );
        when(productService.getActiveProduct(7L)).thenReturn(Optional.of(product));

        ModelAndView result = productsController.productDetails(7L);

        assertThat(result.getViewName()).isEqualTo("product-details");
        assertThat(result.getModel().get("product")).isSameAs(product);
    }

    @Test
    void productDetailsAddsSeoWhenConfigured() {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "Product", "SKU-1", "Category", new BigDecimal("10.00"),
                "Description", 2, List.of("/image.png")
        );
        SeoDto seo = new SeoDto();
        seo.setTitle("Search title");
        when(productService.getActiveProduct(7L)).thenReturn(Optional.of(product));
        when(seoService.getForProduct(7L)).thenReturn(Optional.of(seo));

        ModelAndView result = productsController.productDetails(7L);

        assertThat(result.getModel().get("seo")).isSameAs(seo);
    }

    @Test
    void productDetailsPageInitializesCsrfTokenBeforeRenderingQuickOrderForm() {
        ProductDetailsDto product = new ProductDetailsDto(
                7L, "Product", "SKU-1", "Category", new BigDecimal("10.00"),
                "Description", 2, List.of("/image.png")
        );
        when(productService.getActiveProduct(7L)).thenReturn(Optional.of(product));
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
                "Description", 1, List.of()
        );
        when(productService.getActiveProduct(8L)).thenReturn(Optional.of(product));

        ModelAndView result = productsController.productDetailsPage(8L, new MockHttpServletRequest());

        assertThat(result.getViewName()).isEqualTo("product-details");
        assertThat(result.getModel().get("product")).isSameAs(product);
    }

    @Test
    void productDetailsPageReturnsNotFoundForMissingProduct() {
        when(productService.getActiveProduct(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productsController.productDetailsPage(
                404L, new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void productDetailsReturnsNotFoundForMissingOrInactiveProduct() {
        when(productService.getActiveProduct(404L)).thenReturn(Optional.empty());

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
        assertThat(bindingResult.getFieldError("sku"))
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

        ModelAndView result = productsController.editProduct(
                9L, submitted, bindingResult(submitted), redirect);

        verify(productService).update(9L, submitted);
        assertThat(result.getViewName()).isEqualTo("redirect:/products/9");
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
        assertThat(binding.getFieldError("sku")).isNotNull()
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
