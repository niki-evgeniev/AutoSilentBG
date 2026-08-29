package nevg.autosilent.Models.Dto;

import java.math.BigDecimal;

public record AdminProductInformationSummaryDto(
        long productCount,
        long activeProductCount,
        long totalStock,
        long totalSold,
        long totalViews,
        BigDecimal totalStockValue
) {
}
