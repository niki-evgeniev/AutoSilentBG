package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.UserOrderSummaryDto;

import java.util.List;

public interface UserOrderService {

    List<UserOrderSummaryDto> getOrders(String userEmail);
}
