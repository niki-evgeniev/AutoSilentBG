package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.EcontOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EcontOfficeRepository extends JpaRepository<EcontOffice, Long> {
    List<EcontOffice> findByCityIdAndActiveTrueOrderByNameAsc(Long cityId);
}
