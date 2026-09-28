package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Models.Entity.CatalogSeo;
import nevg.autosilent.Repository.CatalogSeoRepository;
import nevg.autosilent.Service.CatalogSeoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogSeoServiceImpl implements CatalogSeoService {

    private static final String CATALOG_NAME = "Каталог /shumoizolaciya";
    private static final String DEFAULT_TITLE = "Автомобилна шумоизолация и виброизолация | AutoSilent.bg";
    private static final String DEFAULT_DESCRIPTION = "Разгледайте материали Vibrofiltr за шумоизолация, звукоизолация и виброизолация на автомобили, бусове и камиони. Сравнете продукти, цени и наличности.";

    private final CatalogSeoRepository repository;

    public CatalogSeoServiceImpl(CatalogSeoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public SeoDto get() {
        return repository.findFirstByOrderByIdAsc().map(this::toDto).orElseGet(this::defaults);
    }

    @Override
    @Transactional
    public void save(SeoDto request) {
        CatalogSeo seo = repository.findFirstByOrderByIdAsc().orElseGet(CatalogSeo::new);
        seo.setTitle(request.getTitle().trim());
        seo.setDescription(request.getDescription().trim());
        seo.setKeywords(trimToNull(request.getKeywords()));
        seo.setImageUrl(trimToNull(request.getImageUrl()));
        repository.save(seo);
    }

    private SeoDto defaults() {
        SeoDto dto = new SeoDto();
        dto.setProductName(CATALOG_NAME);
        dto.setTitle(DEFAULT_TITLE);
        dto.setDescription(DEFAULT_DESCRIPTION);
        return dto;
    }

    private SeoDto toDto(CatalogSeo seo) {
        SeoDto dto = new SeoDto();
        dto.setProductName(CATALOG_NAME);
        dto.setTitle(seo.getTitle());
        dto.setDescription(seo.getDescription());
        dto.setKeywords(seo.getKeywords());
        dto.setImageUrl(seo.getImageUrl());
        return dto;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
