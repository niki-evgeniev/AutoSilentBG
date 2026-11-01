package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemOrderDto(
        @NotNull Long productId,
        @Min(1) int quantity,
        Long promotionId
) {
    public CartItemOrderDto(Long productId, int quantity) { this(productId, quantity, null); }
}
