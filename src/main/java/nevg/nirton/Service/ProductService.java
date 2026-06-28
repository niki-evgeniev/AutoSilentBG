package nevg.nirton.Service;

import nevg.nirton.Models.Dto.ProductCreateDto;

public interface ProductService {
    void create(ProductCreateDto product, String ownerEmail);
}
