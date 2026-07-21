package nevg.autosilent.Models.Dto;

import nevg.autosilent.Models.Enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserOrderSummaryDto(
        String orderNumber,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt
) {
}
