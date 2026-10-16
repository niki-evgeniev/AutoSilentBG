package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.KitComponentDto;
import nevg.autosilent.Models.Dto.ProductCreateDto;
import nevg.autosilent.Models.Entity.Kit;
import nevg.autosilent.Models.Entity.Category;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Repository.CategoryRepository;
import nevg.autosilent.Repository.KitRepository;
import nevg.autosilent.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class KitService {
    private final KitRepository kits;
    private final ProductRepository products;
    private final ProductService productService;
    private final CategoryRepository categories;

    public KitService(KitRepository kits, ProductRepository products, ProductService productService,
                      CategoryRepository categories) {
        this.kits = kits;
        this.products = products;
        this.productService = productService;
        this.categories = categories;
    }

    @Transactional
    public void create(ProductCreateDto request, String ownerEmail) {
        List<Long> categoryIds = request.getKitCategoryIds().stream().filter(Objects::nonNull).toList();
        if (categoryIds.size() > 3 || categoryIds.stream().distinct().count() != categoryIds.size()) {
            throw new IllegalArgumentException("Изберете до три различни категории за кита.");
        }
        List<Category> selectedCategories = categories.findAllById(categoryIds);
        if (selectedCategories.size() != categoryIds.size() || selectedCategories.stream()
                .anyMatch(category -> "Кит".equalsIgnoreCase(category.getCategory()))) {
            throw new IllegalArgumentException("Изберете валидни категории, различни от „Кит“.");
        }
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
        request.setSecondaryCategoryId(null);
        productService.create(request, ownerEmail);
        Product catalogProduct = products.findByNameProductIgnoreCaseAndModelIgnoreCase(
                request.getNameProduct().trim(), request.getModel().trim()).orElseThrow();
        catalogProduct.getAdditionalCategories().addAll(selectedCategories);
        products.save(catalogProduct);
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
