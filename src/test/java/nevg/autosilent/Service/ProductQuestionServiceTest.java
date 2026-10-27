package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.*;
import nevg.autosilent.Models.Entity.Product;
import nevg.autosilent.Repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class ProductQuestionServiceTest {
    final ContactInquiryService inquiries = mock(ContactInquiryService.class);
    final ContactInquiryRepository repository = mock(ContactInquiryRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final ProductQuestionService service = new ProductQuestionService(inquiries, repository, products);
    final ProductQuestionDto request = new ProductQuestionDto("Ivan", "0888123456", "ivan@example.com", "Is this suitable for my car?");

    ProductQuestionServiceTest() {
        var product = new Product(); product.setId(1L); product.setNameProduct("Brand");
        product.setModel("Model"); product.setSku("SKU-1"); product.setUrl("brand-model");
        when(products.findById(1L)).thenReturn(Optional.of(product));
    }

    @Test void questionIncludesProductLinkSkuAndPhoneInAdminInquiries() {
        service.create(1L, request, "127.0.0.1");
        var captured = ArgumentCaptor.forClass(ContactInquiryDto.class);
        verify(inquiries).create(captured.capture(), eq("127.0.0.1"));
        assertThat(captured.getValue().getSubject()).isEqualTo("[QUESTION] Brand Model");
        assertThat(captured.getValue().getMessage()).contains("/shumoizolaciya/brand-model", "SKU-1", request.phone(), request.question());
    }

    @Test void repeatedQuestionFromSameIpIsThrottled() {
        when(repository.existsByIpAddressAndSubjectStartingWithAndCreatedAtAfter(eq("127.0.0.1"), eq("[QUESTION]"), any())).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, request, "127.0.0.1")).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(429));
        verifyNoInteractions(inquiries);
    }
}
