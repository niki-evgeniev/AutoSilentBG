package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.AdminOrderDetailDto;
import nevg.autosilent.Models.Dto.AdminOrderSummaryDto;
import nevg.autosilent.Models.Enums.OrderStatus;

import java.util.List;

public interface AdminOrderService {

    List<AdminOrderSummaryDto> getAllOrders();

    AdminOrderDetailDto getOrder(Long orderId);

    void updateStatus(Long orderId, OrderStatus newStatus, String note, String changedByEmail);
}
