package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.EcontCity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EcontCityRepository extends JpaRepository<EcontCity, Long> {
    List<EcontCity> findByActiveTrueOrderByNameAsc();
}
