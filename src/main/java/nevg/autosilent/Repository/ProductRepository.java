package nevg.autosilent.Repository;

import nevg.autosilent.Models.Dto.SitemapProductDto;
import nevg.autosilent.Models.Entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            select new nevg.autosilent.Models.Dto.SitemapProductDto(
                p.url, coalesce(p.contentUpdatedAt, p.addDate))
            from Product p
            where p.active = true
            order by p.url
            """)
    List<SitemapProductDto> findAllActiveForSitemap();

    boolean existsByNameProductIgnoreCaseAndModelIgnoreCase(String nameProduct, String model);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByUrl(String url);

    @EntityGraph(attributePaths = "pictures")
    List<Product> findAllByActiveTrueOrderByAddDateDesc();

    @EntityGraph(attributePaths = "pictures")
    List<Product> findTop4ByActiveTrueOrderBySoldDescAddDateDesc();

    Page<Product> findAllByActiveTrue(Pageable pageable);

    Page<Product> findAllByActiveTrueAndCategoryId(Long categoryId, Pageable pageable);

    @Query(value = """
            select p from Product p
            where p.active = true
              and (:categoryId is null or p.category.id = :categoryId)
              and (:brand is null or lower(p.nameProduct) = lower(:brand))
              and (:model is null or lower(p.model) = lower(:model))
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
              and (:inStock = false or p.stock > 0)
              and (:search is null or
                   lower(p.nameProduct) like lower(concat('%', :search, '%')) or
                   lower(p.model) like lower(concat('%', :search, '%')) or
                   lower(p.sku) like lower(concat('%', :search, '%')) or
                   lower(p.category.category) like lower(concat('%', :search, '%')) or
                   lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
                    select count(p) from Product p
                    where p.active = true
                      and (:categoryId is null or p.category.id = :categoryId)
                      and (:brand is null or lower(p.nameProduct) = lower(:brand))
                      and (:model is null or lower(p.model) = lower(:model))
                      and (:minPrice is null or p.price >= :minPrice)
                      and (:maxPrice is null or p.price <= :maxPrice)
                      and (:inStock = false or p.stock > 0)
                      and (:search is null or
                           lower(p.nameProduct) like lower(concat('%', :search, '%')) or
                           lower(p.model) like lower(concat('%', :search, '%')) or
                           lower(p.sku) like lower(concat('%', :search, '%')) or
                           lower(p.category.category) like lower(concat('%', :search, '%')) or
                           lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))
                    """)
    Page<Product> filterActive(@Param("search") String search,
                               @Param("brand") String brand,
                               @Param("model") String model,
                               @Param("minPrice") BigDecimal minPrice,
                               @Param("maxPrice") BigDecimal maxPrice,
                               @Param("inStock") boolean inStock,
                               @Param("categoryId") Long categoryId,
                               Pageable pageable);

    @Query("""
            select distinct p.nameProduct from Product p
            where p.active = true
            order by p.nameProduct
            """)
    List<String> findDistinctActiveBrands();

    @Query("""
            select distinct p.model from Product p
            where p.active = true
              and p.model is not null
              and trim(p.model) <> ''
            order by p.model
            """)
    List<String> findDistinctActiveModels();

    @EntityGraph(attributePaths = "pictures")
    @Query("""
            select p from Product p
            where p.active = true and (
                lower(p.nameProduct) like lower(concat('%', :search, '%')) or
                lower(p.model) like lower(concat('%', :search, '%')) or
                lower(p.sku) like lower(concat('%', :search, '%')) or
                        lower(p.category.category) like lower(concat('%', :search, '%')) or
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
                lower(p.category.category) like lower(concat('%', :search, '%')) or
                lower(coalesce(p.description, '')) like lower(concat('%', :search, '%'))
            )
            """,
            countQuery = """
                    select count(p) from Product p
                    where p.active = true and (
                        lower(p.nameProduct) like lower(concat('%', :search, '%')) or
                        lower(p.model) like lower(concat('%', :search, '%')) or
                        lower(p.sku) like lower(concat('%', :search, '%')) or
                lower(p.category.category) like lower(concat('%', :search, '%')) or
                        lower(coalesce(p.description, '')) like lower(concat('%', :search, '%'))
                    )
                    """)
    Page<Product> searchActive(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = "pictures")
    Optional<Product> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = "pictures")
    Optional<Product> findByUrlAndActiveTrue(String url);

    @EntityGraph(attributePaths = "pictures")
    Optional<Product> findWithPicturesById(Long id);

    boolean existsByNameProductIgnoreCaseAndModelIgnoreCaseAndIdNot(String nameProduct, String model, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct p from Product p left join fetch p.pictures where p.id = :id and p.active = true")
    Optional<Product> findActiveByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct p from Product p left join fetch p.pictures where p.url = :url and p.active = true")
    Optional<Product> findActiveByUrlForUpdate(@Param("url") String url);
}
