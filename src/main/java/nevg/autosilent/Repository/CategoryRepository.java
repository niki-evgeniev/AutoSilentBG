package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Models.Dto.SitemapCategoryDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByCategoryIgnoreCase(String category);

    Optional<Category> findByCategoryIgnoreCase(String category);

    List<Category> findAllByOrderByCategoryAsc();

    @Query("""
            select new nevg.autosilent.Models.Dto.SitemapCategoryDto(
                c.id, c.category, max(coalesce(p.contentUpdatedAt, p.addDate)))
            from Category c, Product p
            where (p.category = c or p.secondaryCategory = c)
              and p.active = true
            group by c.id, c.category
            order by c.category
            """)
    List<SitemapCategoryDto> findAllWithActiveProductsForSitemap();
}
