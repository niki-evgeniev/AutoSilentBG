package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Models.Dto.ProductViewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    void create(ProductCreateDto product, String ownerEmail);

    ProductCreateDto getForEdit(Long id);

    void update(Long id, ProductCreateDto product);

    void delete(Long id);

    List<ProductViewDto> getActiveProducts();

    List<ProductViewDto> getBestSellingProducts();

    List<ProductViewDto> searchActiveProducts(String search);

    Page<ProductViewDto> searchActiveProducts(String search, Pageable pageable);

    Page<ProductViewDto> getActiveProductsByCategory(Long categoryId, Pageable pageable);

    Optional<ProductDetailsDto> getActiveProduct(Long id);

    Optional<ProductDetailsDto> getActiveProductAndIncrementCount(Long id);

    Optional<ProductDetailsDto> getActiveProductByUrl(String url);

    Optional<ProductDetailsDto> getActiveProductByUrlAndIncrementCount(String url);

    Optional<String> getActiveProductUrl(Long id);

    Optional<String> getActiveProductUrlByPreviousUrl(String previousUrl);

    void addVibrofltr();
}
