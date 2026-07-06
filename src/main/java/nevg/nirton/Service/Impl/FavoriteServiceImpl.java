package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.ProductViewDto;
import nevg.nirton.Models.Entity.Favorite;
import nevg.nirton.Models.Entity.Picture;
import nevg.nirton.Models.Entity.Product;
import nevg.nirton.Models.Entity.User;
import nevg.nirton.Repository.FavoriteRepository;
import nevg.nirton.Repository.ProductRepository;
import nevg.nirton.Repository.UserRepository;
import nevg.nirton.Service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public FavoriteServiceImpl(FavoriteRepository favoriteRepository,
                               UserRepository userRepository,
                               ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductViewDto> getFavorites(String userEmail) {
        return favoriteRepository
                .findAllByUserEmailIgnoreCaseAndProductActiveTrueOrderByIdDesc(userEmail)
                .stream().map(Favorite::getProduct).map(this::toViewDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getFavoriteProductIds(String userEmail) {
        return favoriteRepository.findAllByUserEmailIgnoreCase(userEmail).stream()
                .map(favorite -> favorite.getProduct().getId())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional
    public void toggle(String userEmail, Long productId) {
        var existing = favoriteRepository.findByUserEmailIgnoreCaseAndProductId(userEmail, productId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        favoriteRepository.save(favorite);
    }

    private ProductViewDto toViewDto(Product product) {
        String imageUrl = product.getPictures().stream().filter(Picture::isMainImage).findFirst()
                .map(picture -> "/ProductImages/"
                        + UriUtils.encodePathSegment(directoryName(product.getNameProduct()), StandardCharsets.UTF_8)
                        + "/" + UriUtils.encodePathSegment(picture.getFileName(), StandardCharsets.UTF_8))
                .orElse(null);
        return new ProductViewDto(product.getId(), product.getNameProduct(), product.getSku(),
                product.getCategory(), product.getPrice(), product.getDescription(), product.getStock(), imageUrl);
    }

    private String directoryName(String productName) {
        String result = Normalizer.normalize(productName.trim(), Normalizer.Form.NFC)
                .replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "-")
                .replaceAll("\\s+", "-").replaceAll("-+", "-")
                .replaceAll("^[. -]+|[. -]+$", "");
        return result.length() > 100 ? result.substring(0, 100) : result;
    }
}
