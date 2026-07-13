package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Dto.ProductDetailsDto;
import nevg.autosilent.Models.Dto.ProductViewDto;

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
