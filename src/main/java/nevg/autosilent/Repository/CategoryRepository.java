package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByCategoryIgnoreCase(String category);

    Optional<Category> findByCategoryIgnoreCase(String category);

    List<Category> findAllByOrderByCategoryAsc();
}
