package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.SeoDto;
import nevg.nirton.Models.Entity.Product;
import nevg.nirton.Models.Entity.Seo;
import nevg.nirton.Repository.ProductRepository;
import nevg.nirton.Repository.SeoRepository;
import nevg.nirton.Service.SeoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public void save(Long productId, SeoDto request) {
        Product product = findProduct(productId);
        Seo seo = seoRepository.findByProductId(productId).orElseGet(Seo::new);
        seo.setProduct(product);
        seo.setTitle(request.getTitle().trim());
        seo.setDescription(request.getDescription().trim());
        seo.setKeywords(trimToNull(request.getKeywords()));
        seo.setImageUrl(trimToNull(request.getImageUrl()));
        seoRepository.save(seo);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    private SeoDto defaults(Product product) {
        SeoDto dto = new SeoDto();
        dto.setProductName(product.getNameProduct());
        dto.setTitle(product.getNameProduct());
        dto.setDescription(product.getDescription());
        return dto;
    }

    private SeoDto toDto(Seo seo) {
        SeoDto dto = new SeoDto();
        dto.setProductName(seo.getProduct().getNameProduct());
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
