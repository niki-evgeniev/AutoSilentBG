package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.OrderEntity;
import nevg.autosilent.Models.Enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findByOrderStatus(OrderStatus orderStatus);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findByOrderNumberContainingIgnoreCaseOrderByCreatedAtDesc(String orderNumber);
}
