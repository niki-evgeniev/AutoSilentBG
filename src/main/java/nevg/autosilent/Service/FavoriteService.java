package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.ProductViewDto;

import java.util.List;
import java.util.Set;

public interface FavoriteService {

    List<ProductViewDto> getFavorites(String userEmail);

    Set<Long> getFavoriteProductIds(String userEmail);

    void toggle(String userEmail, Long productId);
}
