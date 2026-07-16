package nevg.autosilent.Models.Dto;

import java.math.BigDecimal;

public record PromoCodeValidationDto(
        String promoCode,
        BigDecimal promoDiscountPercent,
        BigDecimal totalDiscountPercent
) {
}
