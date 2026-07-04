package nevg.nirton.Models.Dto;

import java.math.BigDecimal;

public record AdminOrderItemDto(
        String productName,
        String productSku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String productImageUrl
) {
}
