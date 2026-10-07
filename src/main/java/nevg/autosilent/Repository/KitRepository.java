package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.Kit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KitRepository extends JpaRepository<Kit, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Kit> findByCatalogProductId(Long productId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Kit> findByCatalogProductUrlAndCatalogProductActiveTrue(String url);
}
