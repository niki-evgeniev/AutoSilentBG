package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.SeoDto;
import nevg.autosilent.Models.Entity.CatalogSeo;
import nevg.autosilent.Repository.CatalogSeoRepository;
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
class CatalogSeoServiceImplTest {

    @Mock
    private CatalogSeoRepository repository;

    @Test
    void getReturnsDefaultsWhenSettingsHaveNotBeenSaved() {
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        SeoDto result = new CatalogSeoServiceImpl(repository).get();

        assertThat(result.getTitle()).isEqualTo(
                "Автомобилна шумоизолация и виброизолация | AutoSilent.bg");
        assertThat(result.getDescription()).isNotBlank();
    }

    @Test
    void saveTrimsAndPersistsCatalogSeo() {
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        SeoDto request = new SeoDto();
        request.setTitle("  New catalog title  ");
        request.setDescription("  New description  ");
        request.setKeywords("  insulation  ");
        request.setImageUrl("  /images/catalog.webp  ");

        new CatalogSeoServiceImpl(repository).save(request);

        ArgumentCaptor<CatalogSeo> captor = ArgumentCaptor.forClass(CatalogSeo.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("New catalog title");
        assertThat(captor.getValue().getDescription()).isEqualTo("New description");
        assertThat(captor.getValue().getKeywords()).isEqualTo("insulation");
        assertThat(captor.getValue().getImageUrl()).isEqualTo("/images/catalog.webp");
    }
}
