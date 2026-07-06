package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Entity.Favorite;
import nevg.nirton.Models.Entity.Product;
import nevg.nirton.Models.Entity.User;
import nevg.nirton.Repository.FavoriteRepository;
import nevg.nirton.Repository.ProductRepository;
import nevg.nirton.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock FavoriteRepository favoriteRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;

    @Test
    void toggleRemovesExistingFavorite() {
        Favorite favorite = new Favorite();
        when(favoriteRepository.findByUserEmailIgnoreCaseAndProductId("user@example.com", 3L))
                .thenReturn(Optional.of(favorite));

        service().toggle("user@example.com", 3L);

        verify(favoriteRepository).delete(favorite);
        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void toggleCreatesFavoriteWhenItDoesNotExist() {
        User user = new User();
        Product product = new Product();
        when(favoriteRepository.findByUserEmailIgnoreCaseAndProductId("user@example.com", 7L))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(product));

        service().toggle("user@example.com", 7L);

        verify(favoriteRepository).save(org.mockito.ArgumentMatchers.argThat(favorite ->
                favorite.getUser() == user && favorite.getProduct() == product));
    }

    private FavoriteServiceImpl service() {
        return new FavoriteServiceImpl(favoriteRepository, userRepository, productRepository);
    }
}
