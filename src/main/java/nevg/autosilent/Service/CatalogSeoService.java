package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.SeoDto;

public interface CatalogSeoService {

    SeoDto get();

    void save(SeoDto seo);
}
