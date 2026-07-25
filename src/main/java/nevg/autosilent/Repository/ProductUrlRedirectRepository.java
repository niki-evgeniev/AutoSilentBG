package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.ProductUrlRedirect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductUrlRedirectRepository extends JpaRepository<ProductUrlRedirect, Long> {

    boolean existsByOldUrl(String oldUrl);

    @Query("""
            select redirect.product.url
            from ProductUrlRedirect redirect
            where redirect.oldUrl = :oldUrl and redirect.product.active = true
            """)
    Optional<String> findActiveProductUrl(@Param("oldUrl") String oldUrl);
}
