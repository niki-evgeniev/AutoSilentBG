package nevg.autosilent.Models.Dto;

import java.util.List;

public record AdminProductInformationPageDto(
        List<AdminProductInformationDto> products,
        AdminProductInformationSummaryDto summary
) {
}
