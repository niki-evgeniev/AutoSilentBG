package nevg.autosilent.Repository;

import nevg.autosilent.Models.Dto.ProductSoldQuantityDto;
import nevg.autosilent.Models.Entity.OrderItemEntity;
import nevg.autosilent.Models.Enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderId(Long orderId);

    @Query("""
            select new nevg.autosilent.Models.Dto.ProductSoldQuantityDto(
                item.product.id, sum(item.quantity))
            from OrderItemEntity item
            where item.product is not null and item.order.orderStatus = :status
            group by item.product.id
            """)
    List<ProductSoldQuantityDto> sumQuantityByProductForStatus(@Param("status") OrderStatus status);
}
