package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Models.Entity.Seo;
import nevg.autosilent.Repository.ProductRepository;
import nevg.autosilent.Repository.SeoRepository;
import nevg.autosilent.Service.SeoService;
import nevg.autosilent.Utility.ProductDescriptionSanitizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SeoServiceImpl implements SeoService {

    private final SeoRepository seoRepository;
    private final ProductRepository productRepository;

    public SeoServiceImpl(SeoRepository seoRepository, ProductRepository productRepository) {
        this.seoRepository = seoRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SeoDto getForEdit(Long productId) {
        Product product = findProduct(productId);
        return seoRepository.findByProductId(productId)
                .map(this::toDto)
                .orElseGet(() -> defaults(product));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SeoDto> getForProduct(Long productId) {
        return seoRepository.findByProductId(productId).map(this::toDto);
    }

    @Override
    @Transactional
    public void createDefaults(Product product, String imageUrl) {
        if (seoRepository.findByProductId(product.getId()).isPresent()) return;

        Seo seo = new Seo();
        seo.setProduct(product);
        seo.setTitle(truncate(product.getDisplayName(), 70));
        seo.setDescription(truncate(
                ProductDescriptionSanitizer.toPlainText(product.getDescription()), 160));
        seo.setImageUrl(trimToNull(imageUrl));
        seoRepository.save(seo);
    }

    @Override
    @Transactional
    public void save(Long productId, SeoDto request) {
        Product product = findProduct(productId);
        Seo seo = seoRepository.findByProductId(productId).orElseGet(Seo::new);
        seo.setProduct(product);
        seo.setTitle(request.getTitle().trim());
        seo.setDescription(request.getDescription().trim());
        seo.setKeywords(trimToNull(request.getKeywords()));
        seo.setImageUrl(trimToNull(request.getImageUrl()));
        product.setContentUpdatedAt(LocalDateTime.now());
        seoRepository.save(seo);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    private SeoDto defaults(Product product) {
        SeoDto dto = new SeoDto();
        dto.setProductName(product.getDisplayName());
        dto.setTitle(product.getDisplayName());
        dto.setDescription(ProductDescriptionSanitizer.toPlainText(product.getDescription()));
        return dto;
    }

    private SeoDto toDto(Seo seo) {
        SeoDto dto = new SeoDto();
        dto.setProductName(seo.getProduct().getDisplayName());
        dto.setTitle(seo.getTitle());
        dto.setDescription(seo.getDescription());
        dto.setKeywords(seo.getKeywords());
        dto.setImageUrl(seo.getImageUrl());
        return dto;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
