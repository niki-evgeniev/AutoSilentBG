package nevg.autosilent.Models.Dto;

import java.math.BigDecimal;

public record ProductViewDto(
        Long id,
        String url,
        String name,
        String model,
        String sku,
        String category,
        BigDecimal price,
        String description,
        int stock,
        String mainImageUrl
) {
    public ProductViewDto(Long id, String name, String sku, String category, BigDecimal price,
                          String description, int stock, String mainImageUrl) {
        this(id, null, name, null, sku, category, price, description, stock, mainImageUrl);
    }

    public String displayName() {
        return model == null || model.isBlank() ? name : name + " " + model;
    }
}
