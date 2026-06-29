package nevg.nirton.Service;

import nevg.nirton.Models.Dto.ProductCreateDto;
import nevg.nirton.Models.Dto.ProductViewDto;

import java.util.List;

public interface ProductService {
    void create(ProductCreateDto product, String ownerEmail);

    List<ProductViewDto> getActiveProducts();
}
