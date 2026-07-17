package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDetailsTemplateTest {

    private static final Path TEMPLATE = Path.of(
            "src/main/resources/templates/product-details.html");

    @Test
    void productCardExposesRequiredCartSnapshotFields() throws IOException {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("class=\"product-details-card tilt-card\"")
                .contains("th:data-product-id=\"${product.id()}\"")
                .contains("th:data-product-name=\"${product.displayName()}\"")
                .contains("th:data-product-price=\"${product.price()}\"")
                .contains("th:data-product-image=\"${product.mainImageUrl()}\"")
                .contains("th:data-product-url=\"@{/products/{id}(id=${product.id()})}\"")
                .doesNotContain("class=\"quick-order-form\"");
    }

    @Test
    void cartControlsUseQuantityAndRespectProductStock() throws IOException {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("id=\"productQuantity\"")
                .contains("type=\"number\"")
                .contains("value=\"1\"")
                .contains("min=\"1\"")
                .contains("th:max=\"${product.stock()}\"")
                .contains("class=\"btn btn-orange product-add-cart\"")
                .contains("th:disabled=\"${product.stock() == 0}\"")
                .contains("#messages.msg('product.addCart')");
    }
}
