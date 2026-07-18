package nevg.autosilent.Utility;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSlugGeneratorTest {

    @Test
    void createsSeoSlugFromBulgarianNameAndDecimalModel() {
        assertThat(ProductSlugGenerator.toSlug("Виброфилтър 4.0"))
                .isEqualTo("vibrofiltar-4-0");
    }

    @Test
    void normalizesEnglishTextAndSpecialCharacters() {
        assertThat(ProductSlugGenerator.toSlug("  Vibrofiltr / Pro 4.0!  "))
                .isEqualTo("vibrofiltr-pro-4-0");
    }

    @Test
    void usesSafeFallbackForEmptyText() {
        assertThat(ProductSlugGenerator.toSlug("---")).isEqualTo("product");
    }
}
