package nevg.nirton.Repository;

import nevg.nirton.Models.Entity.OrderAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderAddressRepository extends JpaRepository<OrderAddressEntity, Long> {

    Optional<OrderAddressEntity> findByOrderId(Long orderId);
}
