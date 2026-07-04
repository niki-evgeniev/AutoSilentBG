package nevg.nirton.Models.Dto;

import nevg.nirton.Models.Enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminOrderSummaryDto(
        Long id,
        String orderNumber,
        String customerName,
        String customerPhone,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        boolean guestOrder
) {
}
