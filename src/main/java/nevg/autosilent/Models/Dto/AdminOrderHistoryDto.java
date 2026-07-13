package nevg.autosilent.Models.Dto;

import nevg.autosilent.Models.Enums.OrderStatus;

import java.time.LocalDateTime;

public record AdminOrderHistoryDto(
        OrderStatus oldStatus,
        OrderStatus newStatus,
        String changedBy,
        String note,
        LocalDateTime changedAt
) {
}
