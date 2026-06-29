package nevg.nirton.Service;

import nevg.nirton.Models.Dto.ProductCreateDto;
import nevg.nirton.Models.Dto.ProductDetailsDto;
import nevg.nirton.Models.Dto.ProductViewDto;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    void create(ProductCreateDto product, String ownerEmail);

    List<ProductViewDto> getActiveProducts();

    Optional<ProductDetailsDto> getActiveProduct(Long id);
}
