package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    Optional<OrderEntity> findByOrderNumberAndUserId(String orderNumber, Long userId);

    List<OrderEntity> findByOrderStatus(OrderStatus orderStatus);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findByOrderNumberContainingIgnoreCaseOrderByCreatedAtDesc(String orderNumber);

    List<OrderEntity> findByOrderStatusOrderByCreatedAtDesc(OrderStatus orderStatus);

    List<OrderEntity> findByOrderNumberContainingIgnoreCaseAndOrderStatusOrderByCreatedAtDesc(
            String orderNumber, OrderStatus orderStatus);
}
