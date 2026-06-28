package nevg.nirton.Repository;

import nevg.nirton.Models.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByNameProductIgnoreCase(String nameProduct);
    boolean existsBySkuIgnoreCase(String sku);
}
