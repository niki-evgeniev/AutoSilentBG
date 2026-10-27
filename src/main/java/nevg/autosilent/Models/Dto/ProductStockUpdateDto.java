package nevg.autosilent.Models.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ProductStockUpdateDto(@NotEmpty @Size(max = 10000) List<@NotNull @Valid Change> changes) {
    public record Change(@NotNull @Positive Long productId,
                         @NotNull @Min(0) Integer originalStock,
                         @NotNull @Min(0) Integer stock) {}
}
