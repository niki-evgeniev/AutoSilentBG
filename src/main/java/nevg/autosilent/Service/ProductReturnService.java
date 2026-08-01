package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.ProductReturnRequestDto;

public interface ProductReturnService {

    void create(ProductReturnRequestDto request, String ipAddress);
}
