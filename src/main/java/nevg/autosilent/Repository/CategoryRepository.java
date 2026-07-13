package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
