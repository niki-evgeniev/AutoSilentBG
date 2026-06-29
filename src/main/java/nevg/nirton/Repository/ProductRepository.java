package nevg.nirton.Repository;

import nevg.nirton.Models.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByNameProductIgnoreCase(String nameProduct);
    boolean existsBySkuIgnoreCase(String sku);

    @EntityGraph(attributePaths = "pictures")
    List<Product> findAllByActiveTrueOrderByAddDateDesc();
}
