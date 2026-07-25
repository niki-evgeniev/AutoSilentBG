package nevg.autosilent.Utility;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDescriptionSanitizerTest {

    @Test
    void sanitizeKeepsProductFormattingAndRemovesExecutableHtml() {
        String html = """
                <h3 onclick="alert(1)">Характеристики</h3>
                <p>Тих <strong>продукт</strong></p>
                <script>alert('xss')</script>
                <a href="javascript:alert(1)" style="color:red">Опасен линк</a>
                <a href="https://example.com" target="_blank">Безопасен линк</a>
                """;

        String result = ProductDescriptionSanitizer.sanitize(html);

        assertThat(result)
                .contains("<h3>Характеристики</h3>")
                .contains("<p>Тих <strong>продукт</strong></p>")
                .contains("href=\"https://example.com\"")
                .contains("rel=\"nofollow noopener noreferrer\"")
                .doesNotContain("script", "onclick", "javascript:", "style=", "target=");
    }

    @Test
    void toPlainTextRemovesTagsAndUnsafeContent() {
        String html = "<p>Първи <strong>ред</strong></p><script>лошо</script><p>Втори ред</p>";

        assertThat(ProductDescriptionSanitizer.toPlainText(html))
                .isEqualTo("Първи ред Втори ред");
    }
}
