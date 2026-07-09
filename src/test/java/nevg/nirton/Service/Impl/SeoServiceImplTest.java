package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.SeoDto;
import nevg.nirton.Models.Entity.Product;
import nevg.nirton.Models.Entity.Seo;
import nevg.nirton.Repository.ProductRepository;
import nevg.nirton.Repository.SeoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeoServiceImplTest {

    @Mock SeoRepository seoRepository;
    @Mock ProductRepository productRepository;

    private SeoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SeoServiceImpl(seoRepository, productRepository);
    }

    @Test
    void saveCreatesSeoForProductAndTrimsValues() {
        Product product = new Product();
        product.setNameProduct("Product");
        when(productRepository.findById(4L)).thenReturn(Optional.of(product));
        when(seoRepository.findByProductId(4L)).thenReturn(Optional.empty());
        SeoDto dto = new SeoDto();
        dto.setTitle("  Search title  ");
        dto.setDescription("  Search description  ");
        dto.setKeywords("   ");

        service.save(4L, dto);

        ArgumentCaptor<Seo> captor = ArgumentCaptor.forClass(Seo.class);
        verify(seoRepository).save(captor.capture());
        assertThat(captor.getValue().getProduct()).isSameAs(product);
        assertThat(captor.getValue().getTitle()).isEqualTo("Search title");
        assertThat(captor.getValue().getDescription()).isEqualTo("Search description");
        assertThat(captor.getValue().getKeywords()).isNull();
    }

    @Test
    void saveUpdatesExistingSeoAndTrimsOptionalImageUrl() {
        Product product = new Product();
        product.setNameProduct("Product");
        Seo existing = new Seo();
        when(productRepository.findById(4L)).thenReturn(Optional.of(product));
        when(seoRepository.findByProductId(4L)).thenReturn(Optional.of(existing));
        SeoDto dto = new SeoDto();
        dto.setTitle("  Updated title  ");
        dto.setDescription("  Updated description  ");
        dto.setKeywords("  audio, car  ");
        dto.setImageUrl("  /images/product.png  ");

        service.save(4L, dto);

        assertThat(existing.getProduct()).isSameAs(product);
        assertThat(existing.getTitle()).isEqualTo("Updated title");
        assertThat(existing.getDescription()).isEqualTo("Updated description");
        assertThat(existing.getKeywords()).isEqualTo("audio, car");
        assertThat(existing.getImageUrl()).isEqualTo("/images/product.png");
        verify(seoRepository).save(existing);
    }

    @Test
    void getForEditUsesProductValuesWhenSeoDoesNotExist() {
        Product product = new Product();
        product.setNameProduct("Product");
        product.setDescription("Product description");
        when(productRepository.findById(8L)).thenReturn(Optional.of(product));
        when(seoRepository.findByProductId(8L)).thenReturn(Optional.empty());

        SeoDto result = service.getForEdit(8L);

        assertThat(result.getProductName()).isEqualTo("Product");
        assertThat(result.getTitle()).isEqualTo("Product");
        assertThat(result.getDescription()).isEqualTo("Product description");
    }

    @Test
    void getForProductMapsExistingSeoWithoutLoadingProductSeparately() {
        Product product = new Product();
        product.setNameProduct("Product");
        Seo seo = new Seo();
        seo.setProduct(product);
        seo.setTitle("Title");
        seo.setDescription("Description");
        seo.setKeywords("keywords");
        seo.setImageUrl("/image.png");
        when(seoRepository.findByProductId(8L)).thenReturn(Optional.of(seo));

        Optional<SeoDto> result = service.getForProduct(8L);

        assertThat(result).hasValueSatisfying(dto -> {
            assertThat(dto.getProductName()).isEqualTo("Product");
            assertThat(dto.getTitle()).isEqualTo("Title");
            assertThat(dto.getDescription()).isEqualTo("Description");
            assertThat(dto.getKeywords()).isEqualTo("keywords");
            assertThat(dto.getImageUrl()).isEqualTo("/image.png");
        });
    }

    @Test
    void getForEditThrowsNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForEdit(404L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
