package nevg.autosilent.Models.Dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailsDto(
        Long id,
        String name,
        String sku,
        String category,
        BigDecimal price,
        String description,
        int stock,
        List<String> imageUrls
) {
    public String mainImageUrl() {
        return imageUrls.isEmpty() ? null : imageUrls.getFirst();
    }
}
