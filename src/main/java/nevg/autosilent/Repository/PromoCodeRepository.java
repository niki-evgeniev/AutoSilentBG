package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    boolean existsByCode(String code);

    Optional<PromoCode> findByCode(String code);

    List<PromoCode> findAllByOrderByCreatedAtDesc();
}
