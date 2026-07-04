package nevg.nirton.Models.Dto;

import nevg.nirton.Models.Enums.DeliveryType;
import nevg.nirton.Models.Enums.OrderStatus;
import nevg.nirton.Models.Enums.PaymentMethod;
import nevg.nirton.Models.Enums.PaymentStatus;

import java.math.BigDecimal;
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
        BigDecimal totalPrice,
        String customerNote,
        String adminNote,
        LocalDateTime createdAt,
        List<AdminOrderItemDto> items,
        List<AdminOrderHistoryDto> history
) {
}
