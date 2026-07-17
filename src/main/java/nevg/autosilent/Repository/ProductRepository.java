package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameProductIgnoreCaseAndModelIgnoreCase(String nameProduct, String model);

    boolean existsBySkuIgnoreCase(String sku);

    @EntityGraph(attributePaths = "pictures")
    List<Product> findAllByActiveTrueOrderByAddDateDesc();

    @EntityGraph(attributePaths = "pictures")
    List<Product> findTop4ByActiveTrueOrderBySoldDescAddDateDesc();

    Page<Product> findAllByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "pictures")
    @Query("""
            select p from Product p
            where p.active = true and (
                lower(p.nameProduct) like lower(concat('%', :search, '%')) or
                lower(p.model) like lower(concat('%', :search, '%')) or
                lower(p.sku) like lower(concat('%', :search, '%')) or
                lower(p.category) like lower(concat('%', :search, '%')) or
                lower(coalesce(p.description, '')) like lower(concat('%', :search, '%'))
            )
            order by p.addDate desc
            """)
    List<Product> searchActive(@Param("search") String search);

    @Query(value = """
            select p from Product p
            where p.active = true and (
                lower(p.nameProduct) like lower(concat('%', :search, '%')) or
                lower(p.model) like lower(concat('%', :search, '%')) or
                lower(p.sku) like lower(concat('%', :search, '%')) or
                lower(p.category) like lower(concat('%', :search, '%')) or
                lower(coalesce(p.description, '')) like lower(concat('%', :search, '%'))
            )
            """,
            countQuery = """
                    select count(p) from Product p
                    where p.active = true and (
                        lower(p.nameProduct) like lower(concat('%', :search, '%')) or
                        lower(p.model) like lower(concat('%', :search, '%')) or
                        lower(p.sku) like lower(concat('%', :search, '%')) or
                        lower(p.category) like lower(concat('%', :search, '%')) or
                        lower(coalesce(p.description, '')) like lower(concat('%', :search, '%'))
                    )
                    """)
    Page<Product> searchActive(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = "pictures")
    Optional<Product> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = "pictures")
    Optional<Product> findWithPicturesById(Long id);

    boolean existsByNameProductIgnoreCaseAndModelIgnoreCaseAndIdNot(String nameProduct, String model, Long id);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct p from Product p left join fetch p.pictures where p.id = :id and p.active = true")
    Optional<Product> findActiveByIdForUpdate(@Param("id") Long id);
}
