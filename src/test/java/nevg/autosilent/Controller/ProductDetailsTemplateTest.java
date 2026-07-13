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
    void quickOrderFormPostsRequiredSnapshotFields() throws IOException {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("class=\"quick-order-form\"")
                .contains("th:action=\"@{/orders/quick}\"")
                .contains("method=\"post\"")
                .contains("name=\"productId\"")
                .contains("name=\"quantity\"")
                .contains("name=\"names\"")
                .contains("name=\"email\"")
                .contains("name=\"phone\"");
    }

    @Test
    void quickOrderInputsUseCorrectBrowserTypesAndAreRequired() throws IOException {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("id=\"quickOrderNames\"")
                .contains("type=\"email\"")
                .contains("type=\"tel\"")
                .contains("th:text=\"#{quickOrder.submit}\"");
        assertThat(html.split("required", -1).length - 1).isGreaterThanOrEqualTo(3);
    }
}
