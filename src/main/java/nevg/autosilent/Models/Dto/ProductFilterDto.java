package nevg.autosilent.Models.Dto;

import java.math.BigDecimal;

public record ProductFilterDto(
        String search,
        String brand,
        String model,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean inStock,
        Long categoryId
) {
    public ProductFilterDto {
        search = normalize(search);
        brand = normalize(brand);
        model = normalize(model);
        minPrice = nonNegative(minPrice);
        maxPrice = nonNegative(maxPrice);
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal previousMin = minPrice;
            minPrice = maxPrice;
            maxPrice = previousMin;
        }
    }

    public boolean hasCatalogFilters() {
        return brand != null || model != null || minPrice != null || maxPrice != null || inStock;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() >= 0 ? value : BigDecimal.ZERO;
    }
}
