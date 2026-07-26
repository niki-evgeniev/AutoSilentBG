package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Models.Dto.ProductFilterDto;
import nevg.autosilent.Models.Dto.ProductViewDto;
import nevg.autosilent.Models.Entity.Picture;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Models.Entity.ProductUrlRedirect;
import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Repository.ProductUrlRedirectRepository;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.Exception.InvalidProductImageException;
import nevg.autosilent.Service.Exception.ProductAlreadyExistsException;
import nevg.autosilent.Service.Exception.ProductCreationException;
import nevg.autosilent.Service.SeoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };
    private static final byte[] JPG = {
            (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00
    };

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductUrlRedirectRepository productUrlRedirectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SeoService seoService;
    @TempDir
    private Path imagesDirectory;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(
                productRepository, productUrlRedirectRepository, userRepository, categoryRepository,
                seoService, "https://autosilent.bg");
        ReflectionTestUtils.setField(productService, "imagesDirectory", imagesDirectory);
        Category category = new Category();
        category.setId(3L);
        category.setCategory("Category");
        org.mockito.Mockito.lenient().when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
    }

    @Test
    void createSavesNormalizedProductAndItsImages() throws IOException {
        ProductCreateDto request = validRequest();
        request.setAdditionalImages(List.of(
                image("second.jpg", JPG),
                new MockMultipartFile("empty", new byte[0]),
                image("third.png", PNG)
        ));
        User owner = new User();
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));

        productService.create(request, "owner@example.com");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getNameProduct()).isEqualTo("Product One");
        assertThat(saved.getModel()).isEqualTo("Model One");
        assertThat(saved.getUrl()).isEqualTo("product-one-model-one");
        assertThat(saved.getSku()).matches("AS-\\d{10}");
        assertThat(saved.getCategory().getCategory()).isEqualTo("Category");
        assertThat(saved.getDescription()).isEqualTo("Useful product description");
        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getPictures()).hasSize(3)
                .allSatisfy(picture -> assertThat(picture.getProduct()).isSameAs(saved));
        assertThat(saved.getPictures()).filteredOn(Picture::isMainImage).hasSize(1);
        ArgumentCaptor<String> imageUrl = ArgumentCaptor.forClass(String.class);
        verify(seoService).createDefaults(eq(saved), imageUrl.capture());
        assertThat(imageUrl.getValue()).matches(
                "https://autosilent\\.bg/ProductImages/Product-One-Model-One/main-[0-9a-f-]{36}\\.png");

        Path productDirectory = imagesDirectory.resolve("Product-One-Model-One");
        assertThat(productDirectory).isDirectory();
        try (var files = Files.list(productDirectory)) {
            assertThat(files).hasSize(3);
        }
    }

    @Test
    void createSanitizesHtmlDescriptionBeforeSaving() {
        ProductCreateDto request = validRequest();
        request.setDescription("""
                <p onclick="alert(1)">Тих <strong>продукт</strong></p>
                <script>alert('xss')</script>
                """);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(new User()));

        productService.create(request, "owner@example.com");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDescription())
                .isEqualTo("<p>Тих <strong>продукт</strong></p>");
    }

    @Test
    void createRejectsAnExistingProductName() {
        ProductCreateDto request = validRequest();
        when(productRepository.existsByNameProductIgnoreCaseAndModelIgnoreCase("Product One", "Model One"))
                .thenReturn(true);

        assertThatThrownBy(() -> productService.create(request, "owner@example.com"))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .extracting(exception -> ((ProductAlreadyExistsException) exception).getField())
                .isEqualTo("model");

        verifyNoInteractions(userRepository);
    }

    @Test
    void createRejectsMissingMainImage() {
        ProductCreateDto request = validRequest();
        request.setMainImage(null);

        assertThatThrownBy(() -> productService.create(request, "owner@example.com"))
                .isInstanceOf(InvalidProductImageException.class);

        verifyNoInteractions(userRepository);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsMoreThanFourAdditionalImages() {
        ProductCreateDto request = validRequest();
        request.setAdditionalImages(List.of(
                image("1.png", PNG), image("2.png", PNG), image("3.png", PNG),
                image("4.png", PNG), image("5.png", PNG)
        ));

        assertThatThrownBy(() -> productService.create(request, "owner@example.com"))
                .isInstanceOf(InvalidProductImageException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void createRejectsUnsupportedImageContent() {
        ProductCreateDto request = validRequest();
        request.setMainImage(image("fake.png", "not an image".getBytes()));

        assertThatThrownBy(() -> productService.create(request, "owner@example.com"))
                .isInstanceOf(InvalidProductImageException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void createRejectsImageLargerThanFiveMegabytes() {
        ProductCreateDto request = validRequest();
        request.setMainImage(new MockMultipartFile("mainImage", "large.png", "image/png",
                new byte[5 * 1024 * 1024 + 1]));

        assertThatThrownBy(() -> productService.create(request, "owner@example.com"))
                .isInstanceOf(InvalidProductImageException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void createRejectsUnknownOwnerWithoutCreatingFiles() {
        ProductCreateDto request = validRequest();
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request, "missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);

        assertThat(imagesDirectory).isEmptyDirectory();
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTranslatesDuplicateSkuFromDatabaseAndDeletesStoredFiles() {
        ProductCreateDto request = validRequest();
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(new User()));
        when(productRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "constraint violation", new SQLException("Duplicate entry 'SKU-1' for key 'products.sku'")));

        assertThatThrownBy(() -> productService.create(request, "owner@example.com"))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .extracting(exception -> ((ProductAlreadyExistsException) exception).getField())
                .isEqualTo("sku");
        assertThat(imagesDirectory).isEmptyDirectory();
    }

    @Test
    void createWrapsOtherDatabaseErrorsAndDeletesStoredFiles() {
        ProductCreateDto request = validRequest();
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(new User()));
        when(productRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "constraint violation", new SQLException("Foreign key constraint failed")));

        assertThatThrownBy(() -> productService.create(request, "owner@example.com"))
                .isInstanceOf(ProductCreationException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        assertThat(imagesDirectory).isEmptyDirectory();
    }

    @Test
    void getActiveProductsMapsProductAndMainImageUrl() {
        Product product = productWithPictures();
        when(productRepository.findAllByActiveTrueOrderByAddDateDesc()).thenReturn(List.of(product));

        List<ProductViewDto> result = productService.getActiveProducts();

        assertThat(result).containsExactly(new ProductViewDto(
                7L, "Phone Case", "CASE-1", "Accessories", new BigDecimal("12.50"),
                "Protective case", 8, "/ProductImages/Phone-Case/main%20image%23.png"
        ));
    }

    @Test
    void getActiveProductsUsesPlainTextForHtmlDescription() {
        Product product = productWithPictures();
        product.setDescription("<p>Тих <strong>продукт</strong></p><script>alert(1)</script>");
        when(productRepository.findAllByActiveTrueOrderByAddDateDesc()).thenReturn(List.of(product));

        List<ProductViewDto> result = productService.getActiveProducts();

        assertThat(result).singleElement()
                .extracting(ProductViewDto::description)
                .isEqualTo("Тих продукт");
    }

    @Test
    void getBestSellingProductsMapsTopSoldProducts() {
        Product product = productWithPictures();
        when(productRepository.findTop4ByActiveTrueOrderBySoldDescAddDateDesc()).thenReturn(List.of(product));

        List<ProductViewDto> result = productService.getBestSellingProducts();

        verify(productRepository).findTop4ByActiveTrueOrderBySoldDescAddDateDesc();
        assertThat(result).extracting(ProductViewDto::id).containsExactly(7L);
    }

    @Test
    void searchActiveProductsTrimsQueryAndMapsResults() {
        Product product = productWithPictures();
        when(productRepository.searchActive("phone")).thenReturn(List.of(product));

        List<ProductViewDto> result = productService.searchActiveProducts("  phone  ");

        verify(productRepository).searchActive("phone");
        assertThat(result).extracting(ProductViewDto::id).containsExactly(7L);
    }

    @Test
    void blankSearchReturnsAllActiveProducts() {
        when(productRepository.findAllByActiveTrueOrderByAddDateDesc()).thenReturn(List.of());

        assertThat(productService.searchActiveProducts("   ")).isEmpty();

        verify(productRepository).findAllByActiveTrueOrderByAddDateDesc();
        verify(productRepository, never()).searchActive(any());
    }

    @Test
    void pagedSearchUsesNineProductsPerPageAndMapsResults() {
        Product product = productWithPictures();
        when(productRepository.searchActive(org.mockito.ArgumentMatchers.eq("phone"),
                org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(new PageImpl<>(List.of(product)));

        var result = productService.searchActiveProducts("  phone  ", org.springframework.data.domain.PageRequest.of(2, 25));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).searchActive(org.mockito.ArgumentMatchers.eq("phone"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(9);
        assertThat(result.getContent()).extracting(ProductViewDto::id).containsExactly(7L);
    }

    @Test
    void pagedBlankSearchLoadsActiveProductsAndKeepsNineProductsPerPage() {
        when(productRepository.findAllByActiveTrue(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        productService.searchActiveProducts("   ", org.springframework.data.domain.PageRequest.of(0, 25));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAllByActiveTrue(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(9);
        verify(productRepository, never()).searchActive(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    void filterActiveProductsPassesAllNormalizedCriteriaAndUsesNineItemsPerPage() {
        Product product = productWithPictures();
        ProductFilterDto filter = new ProductFilterDto(
                "  phone  ", " Phone ", " Case ",
                new BigDecimal("-5"), new BigDecimal("50"), true, 3L);
        when(productRepository.filterActive(
                org.mockito.ArgumentMatchers.eq("phone"),
                org.mockito.ArgumentMatchers.eq("Phone"),
                org.mockito.ArgumentMatchers.eq("Case"),
                org.mockito.ArgumentMatchers.eq(BigDecimal.ZERO),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("50")),
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        var result = productService.filterActiveProducts(filter, PageRequest.of(2, 25));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).filterActive(
                org.mockito.ArgumentMatchers.eq("phone"),
                org.mockito.ArgumentMatchers.eq("Phone"),
                org.mockito.ArgumentMatchers.eq("Case"),
                org.mockito.ArgumentMatchers.eq(BigDecimal.ZERO),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("50")),
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(3L),
                pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(9);
        assertThat(pageable.getValue().getSort().getOrderFor("nameProduct"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
        assertThat(pageable.getValue().getSort().getOrderFor("model"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
        assertThat(pageable.getValue().getSort().getOrderFor("id"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
        assertThat(result.getContent()).extracting(ProductViewDto::id).containsExactly(7L);
    }

    @Test
    void activeBrandAndModelOptionsComeFromRepository() {
        when(productRepository.findDistinctActiveBrands()).thenReturn(List.of("Brand A", "Brand B"));
        when(productRepository.findDistinctActiveModels()).thenReturn(List.of("Model 1", "Model 2"));

        assertThat(productService.getActiveBrands()).containsExactly("Brand A", "Brand B");
        assertThat(productService.getActiveModels()).containsExactly("Model 1", "Model 2");
    }

    @Test
    void getActiveProductMapsAllImageUrls() {
        Product product = productWithPictures();
        when(productRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(product));

        Optional<ProductDetailsDto> result = productService.getActiveProduct(7L);

        assertThat(result).contains(new ProductDetailsDto(
                7L, "Phone Case", "CASE-1", "Accessories", new BigDecimal("12.50"),
                "Protective case", 8, 3L,
                List.of(
                        "/ProductImages/Phone-Case/main%20image%23.png",
                        "/ProductImages/Phone-Case/side.png"
                )
        ));
    }

    @Test
    void getActiveProductSanitizesStoredHtmlDescription() {
        Product product = productWithPictures();
        product.setDescription("<p onclick=\"alert(1)\">Тих <strong>продукт</strong></p><script>alert(1)</script>");
        when(productRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(product));

        ProductDetailsDto result = productService.getActiveProduct(7L).orElseThrow();

        assertThat(result.description()).isEqualTo("<p>Тих <strong>продукт</strong></p>");
        assertThat(result.plainDescription()).isEqualTo("Тих продукт");
    }

    @Test
    void getActiveProductReturnsEmptyWhenProductDoesNotExist() {
        when(productRepository.findByIdAndActiveTrue(404L)).thenReturn(Optional.empty());

        assertThat(productService.getActiveProduct(404L)).isEmpty();
    }

    @Test
    void getActiveProductAndIncrementCountIncrementsAndMapsProduct() {
        Product product = productWithPictures();
        product.setCount(3L);
        when(productRepository.findActiveByIdForUpdate(7L)).thenReturn(Optional.of(product));

        Optional<ProductDetailsDto> result = productService.getActiveProductAndIncrementCount(7L);

        assertThat(product.getCount()).isEqualTo(4L);
        assertThat(result).isPresent()
                .get()
                .extracting(ProductDetailsDto::count)
                .isEqualTo(4L);
    }

    @Test
    void getActiveProductAndIncrementCountReturnsEmptyWhenProductDoesNotExist() {
        when(productRepository.findActiveByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThat(productService.getActiveProductAndIncrementCount(404L)).isEmpty();
    }

    @Test
    void getForEditMapsProductFieldsAndExistingImages() {
        Product product = productWithPictures();
        product.getPictures().get(0).setId(11L);
        product.getPictures().get(1).setId(12L);
        when(productRepository.findWithPicturesById(7L)).thenReturn(Optional.of(product));

        ProductCreateDto result = productService.getForEdit(7L);

        assertThat(result.getNameProduct()).isEqualTo("Phone Case");
        assertThat(result.getCategoryId()).isEqualTo(3L);
        assertThat(result.getPrice()).isEqualByComparingTo("12.50");
        assertThat(result.getDescription()).isEqualTo("Protective case");
        assertThat(result.getStock()).isEqualTo(8);
        assertThat(result.getExistingMainImageId()).isEqualTo(11L);
        assertThat(result.getExistingImages()).hasSize(2);
        assertThat(result.getExistingImages().get(0).url())
                .isEqualTo("/ProductImages/Phone-Case/main%20image%23.png");
        assertThat(result.getExistingImages().get(0).mainImage()).isTrue();
        assertThat(result.getExistingImages().get(1).url())
                .isEqualTo("/ProductImages/Phone-Case/side.png");
    }

    @Test
    void getForEditThrowsWhenProductDoesNotExist() {
        when(productRepository.findWithPicturesById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getForEdit(404L))
                .isInstanceOf(ProductCreationException.class);
    }

    @Test
    void updateRejectsRemovingEveryImage() {
        Product product = productWithPictures();
        product.getPictures().get(0).setId(11L);
        product.getPictures().get(1).setId(12L);
        ProductCreateDto request = validRequest();
        request.setMainImage(null);
        request.setAdditionalImages(List.of());
        request.setRemovedImageIds(List.of(11L, 12L));
        when(productRepository.findWithPicturesById(7L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.update(7L, request))
                .isInstanceOf(InvalidProductImageException.class);

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateStoresPreviousUrlWhenProductNameChanges() {
        Product product = productWithPictures();
        product.setUrl("phone-case");
        product.getPictures().get(0).setId(11L);
        product.getPictures().get(1).setId(12L);
        ProductCreateDto request = validRequest();
        request.setMainImage(null);
        request.setAdditionalImages(List.of());
        request.setExistingMainImageId(11L);
        when(productRepository.findWithPicturesById(7L)).thenReturn(Optional.of(product));

        productService.update(7L, request);

        assertThat(product.getUrl()).isEqualTo("product-one-model-one");
        ArgumentCaptor<ProductUrlRedirect> redirect =
                ArgumentCaptor.forClass(ProductUrlRedirect.class);
        verify(productUrlRedirectRepository).save(redirect.capture());
        assertThat(redirect.getValue().getOldUrl()).isEqualTo("phone-case");
        assertThat(redirect.getValue().getProduct()).isSameAs(product);
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void previousUrlResolvesOnlyThroughActiveProductRedirectRepository() {
        when(productUrlRedirectRepository.findActiveProductUrl("old-phone-case"))
                .thenReturn(Optional.of("new-phone-case"));

        assertThat(productService.getActiveProductUrlByPreviousUrl("old-phone-case"))
                .contains("new-phone-case");
    }

    @Test
    void deleteMarksProductInactive() {
        Product product = new Product();
        product.setActive(true);
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));

        productService.delete(7L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void deleteThrowsWhenProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(404L))
                .isInstanceOf(ProductCreationException.class);

        verify(productRepository, never()).save(any());
    }

    private ProductCreateDto validRequest() {
        ProductCreateDto request = new ProductCreateDto();
        request.setNameProduct("  Product One  ");
        request.setModel("  Model One  ");
        request.setCategoryId(3L);
        request.setPrice(new BigDecimal("19.99"));
        request.setDescription("  Useful product description  ");
        request.setStock(3);
        request.setActive(true);
        request.setMainImage(image("main.png", PNG));
        return request;
    }

    private Product productWithPictures() {
        Product product = new Product();
        product.setId(7L);
        product.setNameProduct("Phone Case");
        product.setSku("CASE-1");
        Category category = new Category();
        category.setId(3L);
        category.setCategory("Accessories");
        product.setCategory(category);
        product.setPrice(new BigDecimal("12.50"));
        product.setDescription("Protective case");
        product.setStock(8);
        product.setCount(3L);

        Picture main = new Picture();
        main.setFileName("main image#.png");
        main.setMainImage(true);
        product.addPicture(main);

        Picture side = new Picture();
        side.setFileName("side.png");
        product.addPicture(side);
        return product;
    }

    private MockMultipartFile image(String fileName, byte[] content) {
        return new MockMultipartFile("image", fileName, "application/octet-stream", content);
    }
}
