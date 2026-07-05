package nevg.nirton.Service;

import nevg.nirton.Models.Dto.ProductCreateDto;
import nevg.nirton.Models.Dto.ProductDetailsDto;
import nevg.nirton.Models.Dto.ProductViewDto;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    void create(ProductCreateDto product, String ownerEmail);

    ProductCreateDto getForEdit(Long id);

    void update(Long id, ProductCreateDto product);

    void delete(Long id);

    List<ProductViewDto> getActiveProducts();

    List<ProductViewDto> searchActiveProducts(String search);

    Optional<ProductDetailsDto> getActiveProduct(Long id);

    void addVibrofltr();
}
