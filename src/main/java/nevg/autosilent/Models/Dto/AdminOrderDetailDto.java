package nevg.autosilent.Models.Dto;

import nevg.autosilent.Models.Enums.DeliveryType;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Models.Enums.PaymentMethod;
import nevg.autosilent.Models.Enums.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderDetailDto(
        Long id,
        String orderNumber,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        String customerPhone,
        boolean guestOrder,
        DeliveryType deliveryType,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        BigDecimal subtotalPrice,
        BigDecimal deliveryPrice,
        BigDecimal discountPrice,
        String promoCode,
        BigDecimal promoDiscountPercent,
        BigDecimal totalPrice,
        String customerNote,
        String adminNote,
        LocalDateTime createdAt,
        List<AdminOrderItemDto> items,
        List<AdminOrderHistoryDto> history
) {
    public BigDecimal discountPercent() {
        if (discountPrice == null || discountPrice.signum() <= 0
                || subtotalPrice == null || subtotalPrice.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return discountPrice.multiply(BigDecimal.valueOf(100))
                .divide(subtotalPrice, 2, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    public String discountPercentText() {
        return discountPercent().toPlainString();
    }
}
