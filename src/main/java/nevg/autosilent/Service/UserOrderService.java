package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.UserOrderSummaryDto;
import nevg.autosilent.Models.Dto.UserOrderDetailDto;

import java.util.List;

public interface UserOrderService {

    List<UserOrderSummaryDto> getOrders(String userEmail);

    UserOrderDetailDto getOrder(String orderNumber, String userEmail);
}
