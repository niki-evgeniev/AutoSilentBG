package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.SeoDto;
import nevg.nirton.Models.Entity.Product;
import nevg.nirton.Models.Entity.Seo;
import nevg.nirton.Repository.ProductRepository;
import nevg.nirton.Repository.SeoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeoServiceImplTest {

    @Mock SeoRepository seoRepository;
    @Mock ProductRepository productRepository;

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

        new SeoServiceImpl(seoRepository, productRepository).save(4L, dto);

        ArgumentCaptor<Seo> captor = ArgumentCaptor.forClass(Seo.class);
        verify(seoRepository).save(captor.capture());
        assertThat(captor.getValue().getProduct()).isSameAs(product);
        assertThat(captor.getValue().getTitle()).isEqualTo("Search title");
        assertThat(captor.getValue().getDescription()).isEqualTo("Search description");
        assertThat(captor.getValue().getKeywords()).isNull();
    }

    @Test
    void getForEditUsesProductValuesWhenSeoDoesNotExist() {
        Product product = new Product();
        product.setNameProduct("Product");
        product.setDescription("Product description");
        when(productRepository.findById(8L)).thenReturn(Optional.of(product));
        when(seoRepository.findByProductId(8L)).thenReturn(Optional.empty());

        SeoDto result = new SeoServiceImpl(seoRepository, productRepository).getForEdit(8L);

        assertThat(result.getProductName()).isEqualTo("Product");
        assertThat(result.getTitle()).isEqualTo("Product");
        assertThat(result.getDescription()).isEqualTo("Product description");
    }
}
