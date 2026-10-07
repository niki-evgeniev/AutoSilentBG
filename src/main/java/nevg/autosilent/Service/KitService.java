package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.KitComponentDto;
import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Entity.Kit;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Repository.KitRepository;
import nevg.autosilent.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class KitService {
    private final KitRepository kits;
    private final ProductRepository products;
    private final ProductService productService;

    public KitService(KitRepository kits, ProductRepository products, ProductService productService) {
        this.kits = kits;
        this.products = products;
        this.productService = productService;
    }

    @Transactional
    public void create(ProductCreateDto request, String ownerEmail) {
        List<KitComponentDto> components = request.getComponents();
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("Добавете поне един продукт в кита.");
        }
        Set<Long> ids = new HashSet<>();
        for (KitComponentDto component : components) {
            if (component == null || component.getProductId() == null ||
                    component.getQuantity() == null || component.getQuantity() < 1 ||
                    !ids.add(component.getProductId())) {
                throw new IllegalArgumentException("Изберете различни продукти с количество поне 1.");
            }
        }
        List<Product> selected = products.findAllById(ids);
        if (selected.size() != ids.size() || selected.stream().anyMatch(product ->
                !product.isActive() || "Кит".equalsIgnoreCase(product.getCategory().getCategory()))) {
            throw new IllegalArgumentException("Китът може да съдържа само активни продукти.");
        }
        // The catalog product is the purchasable identity. Its availability comes from the components.
        request.setStock(Integer.MAX_VALUE);
        productService.create(request, ownerEmail);
        Product catalogProduct = products.findByNameProductIgnoreCaseAndModelIgnoreCase(
                request.getNameProduct().trim(), request.getModel().trim()).orElseThrow();
        Kit kit = new Kit();
        kit.setCatalogProduct(catalogProduct);
        for (KitComponentDto component : components) {
            Product selectedProduct = selected.stream()
                    .filter(product -> product.getId().equals(component.getProductId())).findFirst().orElseThrow();
            kit.addItem(selectedProduct, component.getQuantity());
        }
        kits.save(kit);
    }
}
