package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.PromoCodeCreateDto;
import nevg.autosilent.Models.Dto.PromoCodeViewDto;

import java.math.BigDecimal;
import java.util.List;

public interface PromoCodeService {

    List<Integer> allowedDiscounts();

    List<PromoCodeViewDto> getAll();

    void create(PromoCodeCreateDto request, String adminEmail);

    BigDecimal discountPercent(String code);

    String normalizeCode(String code);
}
