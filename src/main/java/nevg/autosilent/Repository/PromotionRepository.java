package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.Promotion;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    boolean existsBySlug(String slug);
    java.util.Optional<Promotion> findBySlug(String slug);
    @EntityGraph(attributePaths = "product")
    List<Promotion> findAllByOrderByIdAsc();
    @EntityGraph(attributePaths = {"product", "product.category", "product.pictures"})
    List<Promotion> findAllByOrderByIdDesc();
}
