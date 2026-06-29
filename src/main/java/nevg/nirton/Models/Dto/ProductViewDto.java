package nevg.nirton.Models.Dto;

import java.math.BigDecimal;

public record ProductViewDto(
        Long id,
        String name,
        String sku,
        String category,
        BigDecimal price,
        String description,
        int stock,
        String mainImageUrl
) {
}
