package nevg.nirton.Models.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemOrderDto(
        @NotNull Long productId,
        @Min(1) int quantity
) {
}
