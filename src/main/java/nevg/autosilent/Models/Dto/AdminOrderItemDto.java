package nevg.autosilent.Models.Dto;

import java.math.BigDecimal;

public record AdminOrderItemDto(
        String productName,
        String productSku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String productImageUrl,
        Long id
) {
    public AdminOrderItemDto(String productName, String productSku, int quantity, BigDecimal unitPrice,
                             BigDecimal totalPrice, String productImageUrl) {
        this(productName, productSku, quantity, unitPrice, totalPrice, productImageUrl, null);
    }
}
