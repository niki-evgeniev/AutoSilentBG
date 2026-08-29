package nevg.autosilent.Models.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminProductInformationDto(
        Long id,
        String url,
        String name,
        String sku,
        String category,
        BigDecimal price,
        int stock,
        long sold,
        long views,
        BigDecimal stockValue,
        boolean active,
        LocalDateTime addedAt
) {
}
