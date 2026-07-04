package nevg.nirton.Repository;

import nevg.nirton.Models.Entity.OrderEntity;
import nevg.nirton.Models.Enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findByOrderStatus(OrderStatus orderStatus);
}
