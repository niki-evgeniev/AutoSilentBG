package nevg.nirton.Repository;

import nevg.nirton.Models.Entity.Seo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeoRepository extends JpaRepository<Seo, Long> {
    Optional<Seo> findByProductId(Long productId);
}
