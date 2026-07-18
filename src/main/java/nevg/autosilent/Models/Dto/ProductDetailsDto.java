package nevg.autosilent.Models.Dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailsDto(
        Long id,
        String url,
        String name,
        String model,
        String sku,
        String category,
        BigDecimal price,
        String description,
        int stock,
        long count,
        List<String> imageUrls
) {
    public ProductDetailsDto(Long id, String name, String sku, String category, BigDecimal price,
                             String description, int stock, long count, List<String> imageUrls) {
        this(id, null, name, null, sku, category, price, description, stock, count, imageUrls);
    }

    public String displayName() {
        return model == null || model.isBlank() ? name : name + " " + model;
    }

    public String mainImageUrl() {
        return imageUrls.isEmpty() ? null : imageUrls.getFirst();
    }
}
