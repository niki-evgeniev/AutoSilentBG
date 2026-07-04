package nevg.nirton.Models.Dto;

import nevg.nirton.Models.Enums.OrderStatus;

import java.time.LocalDateTime;

public record AdminOrderHistoryDto(
        OrderStatus oldStatus,
        OrderStatus newStatus,
        String changedBy,
        String note,
        LocalDateTime changedAt
) {
}
