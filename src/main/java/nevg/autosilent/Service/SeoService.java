package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Models.Entity.Product;

import java.util.Optional;

public interface SeoService {

    SeoDto getForEdit(Long productId);

    Optional<SeoDto> getForProduct(Long productId);

    void createDefaults(Product product, String imageUrl);

    void save(Long productId, SeoDto seo);
}
