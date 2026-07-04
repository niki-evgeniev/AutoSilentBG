package nevg.nirton.Service;

import nevg.nirton.Models.Dto.AdminOrderDetailDto;
import nevg.nirton.Models.Dto.AdminOrderSummaryDto;
import nevg.nirton.Models.Enums.OrderStatus;

import java.util.List;

public interface AdminOrderService {

    List<AdminOrderSummaryDto> getAllOrders();

    AdminOrderDetailDto getOrder(Long orderId);

    void updateStatus(Long orderId, OrderStatus newStatus, String note, String changedByEmail);
}
