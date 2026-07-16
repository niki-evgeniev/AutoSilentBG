package nevg.autosilent.Models.Dto;

import java.time.LocalDateTime;

public record PromoCodeViewDto(
        Long id,
        String code,
        int discountPercent,
        String createdBy,
        LocalDateTime createdAt
) {
}
