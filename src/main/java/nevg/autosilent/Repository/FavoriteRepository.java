package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.Favorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @EntityGraph(attributePaths = {"product", "product.pictures"})
    List<Favorite> findAllByUserEmailIgnoreCaseAndProductActiveTrueOrderByIdDesc(String email);

    Optional<Favorite> findByUserEmailIgnoreCaseAndProductId(String email, Long productId);

    boolean existsByUserEmailIgnoreCaseAndProductId(String email, Long productId);

    List<Favorite> findAllByUserEmailIgnoreCase(String email);
}
