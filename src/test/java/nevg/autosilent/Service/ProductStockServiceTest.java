package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.ProductStockUpdateDto;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductStockServiceTest {
    final ProductRepository products = mock(ProductRepository.class);
    final KitRepository kits = mock(KitRepository.class);
    final StaticMessageSource messages = new StaticMessageSource();
    final ProductStockService service = new ProductStockService(products, kits, messages);

    ProductStockServiceTest() { messages.setUseCodeAsDefaultMessage(true); }

    @Test void updatesOneProductAndAllowsZeroStock() {
        var product = product(1L, 5);
        assertThat(service.update(request(new ProductStockUpdateDto.Change(1L, 5, 0)))).isEqualTo(1);
        assertThat(product.getStock()).isZero();
    }

    @Test void updatesSeveralProductsIncludingHiddenOnesAndIgnoresUnchangedValues() {
        var first = product(1L, 5); var second = product(2L, 10); var third = product(3L, 3);
        second.setActive(false);
        assertThat(service.update(request(new ProductStockUpdateDto.Change(2L, 10, 20),
                new ProductStockUpdateDto.Change(1L, 5, 8), new ProductStockUpdateDto.Change(3L, 3, 3)))).isEqualTo(2);
        assertThat(first.getStock()).isEqualTo(8);
        assertThat(second.getStock()).isEqualTo(20);
        assertThat(third.getStock()).isEqualTo(3);
        var order = inOrder(products);
        order.verify(products).findByIdForStockUpdate(1L);
        order.verify(products).findByIdForStockUpdate(2L);
        order.verify(products).findByIdForStockUpdate(3L);
    }

    @Test void concurrentOrderChangeRejectsWholeBatchBeforeAnyQuantityIsModified() {
        var first = product(1L, 5); var second = product(2L, 9);
        assertThatThrownBy(() -> service.update(request(new ProductStockUpdateDto.Change(1L, 5, 8),
                new ProductStockUpdateDto.Change(2L, 10, 20))))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThat(first.getStock()).isEqualTo(5);
        assertThat(second.getStock()).isEqualTo(9);
    }

    @Test void rejectsMissingProductsWithoutChangingEarlierRows() {
        var first = product(1L, 5);
        assertThatThrownBy(() -> service.update(request(new ProductStockUpdateDto.Change(1L, 5, 8),
                new ProductStockUpdateDto.Change(2L, 2, 3)))).isInstanceOf(ResponseStatusException.class);
        assertThat(first.getStock()).isEqualTo(5);
    }

    @Test void rejectsNegativeQuantitiesAndDuplicateRowsBeforeAccessingProducts() {
        assertThatThrownBy(() -> service.update(request(new ProductStockUpdateDto.Change(1L, 5, -1))))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.update(request(new ProductStockUpdateDto.Change(1L, 5, 1),
                new ProductStockUpdateDto.Change(1L, 5, 2)))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(products);
    }

    @Test void rejectsDirectKitStockChanges() {
        var product = product(1L, Integer.MAX_VALUE);
        when(kits.existsByCatalogProductId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(request(new ProductStockUpdateDto.Change(1L, Integer.MAX_VALUE, 10))))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(product.getStock()).isEqualTo(Integer.MAX_VALUE);
    }

    Product product(Long id, int stock) {
        var product = new Product(); product.setId(id); product.setNameProduct("Product " + id); product.setStock(stock);
        when(products.findByIdForStockUpdate(id)).thenReturn(Optional.of(product));
        return product;
    }
    ProductStockUpdateDto request(ProductStockUpdateDto.Change... changes) {
        return new ProductStockUpdateDto(List.of(changes));
    }
}
