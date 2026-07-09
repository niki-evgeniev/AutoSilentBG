package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Entity.Favorite;
import nevg.nirton.Models.Entity.Picture;
import nevg.nirton.Models.Entity.Product;
import nevg.nirton.Models.Entity.User;
import nevg.nirton.Repository.FavoriteRepository;
import nevg.nirton.Repository.ProductRepository;
import nevg.nirton.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock FavoriteRepository favoriteRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;

    private FavoriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FavoriteServiceImpl(favoriteRepository, userRepository, productRepository);
    }

    @Test
    void getFavoritesReturnsActiveProductsAsViewDtosWithMainImageUrl() {
        Product product = product(5L, "  Car / Part <One>  ", "SKU-5");
        Picture secondaryPicture = picture("side.jpg", false);
        Picture mainPicture = picture("main image 1.jpg", true);
        product.addPicture(secondaryPicture);
        product.addPicture(mainPicture);
        Favorite favorite = new Favorite();
        favorite.setProduct(product);
        when(favoriteRepository.findAllByUserEmailIgnoreCaseAndProductActiveTrueOrderByIdDesc("user@example.com"))
                .thenReturn(List.of(favorite));

        var result = service.getFavorites("user@example.com");

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(5L);
            assertThat(dto.name()).isEqualTo("  Car / Part <One>  ");
            assertThat(dto.sku()).isEqualTo("SKU-5");
            assertThat(dto.category()).isEqualTo("Exhaust");
            assertThat(dto.price()).isEqualByComparingTo("42.50");
            assertThat(dto.description()).isEqualTo("Quiet part");
            assertThat(dto.stock()).isEqualTo(9);
            assertThat(dto.mainImageUrl()).isEqualTo("/ProductImages/Car-Part-One/main%20image%201.jpg");
        });
    }

    @Test
    void getFavoritesReturnsNullImageUrlWhenProductHasNoMainImage() {
        Product product = product(6L, "Silencer", "SKU-6");
        product.addPicture(picture("side.jpg", false));
        Favorite favorite = new Favorite();
        favorite.setProduct(product);
        when(favoriteRepository.findAllByUserEmailIgnoreCaseAndProductActiveTrueOrderByIdDesc("user@example.com"))
                .thenReturn(List.of(favorite));

        var result = service.getFavorites("user@example.com");

        assertThat(result).singleElement().satisfies(dto -> assertThat(dto.mainImageUrl()).isNull());
    }

    @Test
    void getFavoriteProductIdsReturnsUniqueUnmodifiableProductIds() {
        Favorite first = favorite(product(1L, "First", "SKU-1"));
        Favorite duplicate = favorite(product(1L, "First duplicate", "SKU-1-DUP"));
        Favorite second = favorite(product(2L, "Second", "SKU-2"));
        when(favoriteRepository.findAllByUserEmailIgnoreCase("user@example.com"))
                .thenReturn(List.of(first, duplicate, second));

        Set<Long> result = service.getFavoriteProductIds("user@example.com");

        assertThat(result).containsExactlyInAnyOrder(1L, 2L);
        assertThatThrownBy(() -> result.add(3L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toggleRemovesExistingFavorite() {
        Favorite favorite = new Favorite();
        when(favoriteRepository.findByUserEmailIgnoreCaseAndProductId("user@example.com", 3L))
                .thenReturn(Optional.of(favorite));

        service.toggle("user@example.com", 3L);

        verify(favoriteRepository).delete(favorite);
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void toggleCreatesFavoriteWhenItDoesNotExist() {
        User user = new User();
        Product product = new Product();
        when(favoriteRepository.findByUserEmailIgnoreCaseAndProductId("user@example.com", 7L))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(product));

        service.toggle("user@example.com", 7L);

        verify(favoriteRepository).save(org.mockito.ArgumentMatchers.argThat(favorite ->
                favorite.getUser() == user && favorite.getProduct() == product));
    }

    @Test
    void toggleThrowsUnauthorizedWhenUserDoesNotExist() {
        when(favoriteRepository.findByUserEmailIgnoreCaseAndProductId("missing@example.com", 7L))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggle("missing@example.com", 7L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(productRepository, never()).findByIdAndActiveTrue(any());
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void toggleThrowsNotFoundWhenProductDoesNotExistOrIsInactive() {
        User user = new User();
        when(favoriteRepository.findByUserEmailIgnoreCaseAndProductId("user@example.com", 404L))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndActiveTrue(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggle("user@example.com", 404L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(favoriteRepository, never()).save(any());
    }

    private Favorite favorite(Product product) {
        Favorite favorite = new Favorite();
        favorite.setProduct(product);
        return favorite;
    }

    private Product product(Long id, String name, String sku) {
        Product product = new Product();
        product.setId(id);
        product.setNameProduct(name);
        product.setSku(sku);
        product.setCategory("Exhaust");
        product.setPrice(new BigDecimal("42.50"));
        product.setDescription("Quiet part");
        product.setStock(9);
        return product;
    }

    private Picture picture(String fileName, boolean mainImage) {
        Picture picture = new Picture();
        picture.setFileName(fileName);
        picture.setMainImage(mainImage);
        return picture;
    }
}
