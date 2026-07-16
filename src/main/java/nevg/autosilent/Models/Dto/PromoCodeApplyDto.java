package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.Size;

public record PromoCodeApplyDto(
        @Size(max = 40) String promoCode
) {
}
