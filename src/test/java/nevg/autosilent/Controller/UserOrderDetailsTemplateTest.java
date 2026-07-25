package nevg.autosilent.Controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UserOrderDetailsTemplateTest {

    @Test
    void orderHistoryLinksToDetailsAndDetailsShowPurchasedItems() throws Exception {
        String history = template("user-orders.html");
        String details = template("user-order-details.html");

        assertThat(history)
                .contains("th:href=\"@{/user/orders/{orderNumber}(orderNumber=${order.orderNumber()})}\"");
        assertThat(details)
                .contains("<meta name=\"robots\" content=\"noindex,nofollow\">")
                .containsSubsequence(
                        "th:text=\"#{checkout.products}\"",
                        "order.createdAt()",
                        "th:each=\"item : ${order.items()}\"")
                .contains("th:each=\"item : ${order.items()}\"")
                .contains("${item.quantity()}")
                .contains("item.unitPrice()")
                .contains("${item.totalPrice()}")
                .contains("${order.totalPrice()}")
                .doesNotContain("account.order.details")
                .doesNotContain("account.order.deliveryType")
                .doesNotContain("account.order.paymentMethod")
                .doesNotContain("account.order.paymentStatus");
    }

    private String template(String name) throws Exception {
        return Files.readString(
                Path.of("src/main/resources/templates", name), StandardCharsets.UTF_8);
    }
}
