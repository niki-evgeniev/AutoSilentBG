package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.ProductStockUpdateDto;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Repository.KitRepository;
import nevg.autosilent.Repository.ProductRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class ProductStockService {
    private final ProductRepository products;
    private final KitRepository kits;
    private final MessageSource messages;

    public ProductStockService(ProductRepository products, KitRepository kits, MessageSource messages) {
        this.products = products;
        this.kits = kits;
        this.messages = messages;
    }

    @Transactional
    public int update(ProductStockUpdateDto request) {
        if (request == null || request.changes() == null || request.changes().isEmpty()
                || request.changes().size() > 10000) throw error(HttpStatus.BAD_REQUEST, "stock.invalid");
        Set<Long> ids = new HashSet<>();
        for (var change : request.changes()) {
            if (change == null || change.productId() == null || change.productId() <= 0
                    || change.originalStock() == null || change.originalStock() < 0
                    || change.stock() == null || change.stock() < 0 || !ids.add(change.productId())) {
                throw error(HttpStatus.BAD_REQUEST, "stock.invalid");
            }
        }
        Map<Product, Integer> updates = new LinkedHashMap<>();
        // Use the same row locks as checkout, in a consistent order for batch updates.
        for (var change : request.changes().stream().sorted(Comparator.comparing(ProductStockUpdateDto.Change::productId)).toList()) {
            Product product = products.findByIdForStockUpdate(change.productId())
                    .orElseThrow(() -> error(HttpStatus.CONFLICT, "stock.missing"));
            if (kits.existsByCatalogProductId(product.getId())) {
                throw error(HttpStatus.BAD_REQUEST, "stock.kit");
            }
            if (product.getStock() != change.originalStock()) {
                throw error(HttpStatus.CONFLICT, "stock.conflict", product.getDisplayName());
            }
            if (product.getStock() != change.stock()) updates.put(product, change.stock());
        }
        // Validate the entire batch before modifying any managed entity.
        updates.forEach(Product::setStock);
        return updates.size();
    }

    private ResponseStatusException error(HttpStatus status, String key, Object... arguments) {
        return new ResponseStatusException(status, messages.getMessage(key, arguments, LocaleContextHolder.getLocale()));
    }
}
