package nevg.autosilent.Models.Dto;

import nevg.autosilent.Models.Enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UserOrderDetailDto(
        String orderNumber,
        OrderStatus orderStatus,
        BigDecimal subtotalPrice,
        BigDecimal deliveryPrice,
        BigDecimal discountPrice,
        String promoCode,
        BigDecimal promoDiscountPercent,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        List<AdminOrderItemDto> items
) {
}
