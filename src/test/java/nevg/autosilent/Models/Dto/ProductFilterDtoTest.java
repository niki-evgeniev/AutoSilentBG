package nevg.autosilent.Models.Dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductFilterDtoTest {

    @Test
    void normalizesTextNegativePricesAndReversedPriceRange() {
        ProductFilterDto filter = new ProductFilterDto(
                "  search  ", "  Brand  ", " Model ",
                new BigDecimal("100"), new BigDecimal("-5"), true, 3L);

        assertThat(filter.search()).isEqualTo("search");
        assertThat(filter.brand()).isEqualTo("Brand");
        assertThat(filter.model()).isEqualTo("Model");
        assertThat(filter.minPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(filter.maxPrice()).isEqualByComparingTo("100");
        assertThat(filter.hasCatalogFilters()).isTrue();
    }
}
