package nevg.autosilent.Controller;

import nevg.autosilent.Service.*;
import nevg.autosilent.Models.Dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminProductInformationControllerTest {
    final AdminProductInformationService information = mock(AdminProductInformationService.class);
    final ProductStockService stocks = mock(ProductStockService.class);
    final AdminProductInformationController controller = new AdminProductInformationController(information, stocks);

    @Test void updateReturnsNumberOfChangedProducts() {
        var request = new ProductStockUpdateDto(List.of(new ProductStockUpdateDto.Change(1L, 5, 10)));
        when(stocks.update(request)).thenReturn(1);
        var response = controller.updateStock(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("updated", 1));
    }

    @Test void conflictReturnsUsefulMessageAndKeepsErrorStatus() {
        var request = new ProductStockUpdateDto(List.of(new ProductStockUpdateDto.Change(1L, 5, 10)));
        when(stocks.update(request)).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Reload the page"));
        var response = controller.updateStock(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Reload the page"));
    }
}
