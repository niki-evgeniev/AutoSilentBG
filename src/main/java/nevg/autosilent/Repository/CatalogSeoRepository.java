package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.CatalogSeo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatalogSeoRepository extends JpaRepository<CatalogSeo, Long> {

    Optional<CatalogSeo> findFirstByOrderByIdAsc();
}
