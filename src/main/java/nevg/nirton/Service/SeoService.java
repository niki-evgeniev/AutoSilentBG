package nevg.nirton.Service;

import nevg.nirton.Models.Dto.SeoDto;

import java.util.Optional;

public interface SeoService {

    SeoDto getForEdit(Long productId);

    Optional<SeoDto> getForProduct(Long productId);

    void save(Long productId, SeoDto seo);
}
