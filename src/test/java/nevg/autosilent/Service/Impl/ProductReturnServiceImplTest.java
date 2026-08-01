package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.ContactInquiryDto;
import nevg.autosilent.Models.Dto.ProductReturnRequestDto;
import nevg.autosilent.Models.Enums.ReturnResolution;
import nevg.autosilent.Service.ContactInquiryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductReturnServiceImplTest {

    @Mock ContactInquiryService contactInquiryService;

    @Test
    void createStoresStructuredReturnAsAdminInquiry() {
        ProductReturnRequestDto request = new ProductReturnRequestDto();
        request.setFirstName("  Ivan  ");
        request.setLastName("  Ivanov  ");
        request.setEmail("  IVAN@EXAMPLE.COM  ");
        request.setPhone("  +359 89 123 4567  ");
        request.setOrderNumber("  nrt-20260101123000-ab12cd34  ");
        request.setOrderedOn(LocalDate.of(2026, 1, 1));
        request.setProductName("  Sound insulation model X  ");
        request.setProductCode("  SKU-100  ");
        request.setQuantity(2);
        request.setResolution(ReturnResolution.EXCHANGE);
        request.setReason("  The product does not fit my vehicle.  ");

        new ProductReturnServiceImpl(contactInquiryService).create(request, "203.0.113.9");

        ArgumentCaptor<ContactInquiryDto> captor = ArgumentCaptor.forClass(ContactInquiryDto.class);
        verify(contactInquiryService).create(captor.capture(), org.mockito.ArgumentMatchers.eq("203.0.113.9"));
        ContactInquiryDto inquiry = captor.getValue();
        assertThat(inquiry.getName()).isEqualTo("Ivan Ivanov");
        assertThat(inquiry.getEmail()).isEqualTo("IVAN@EXAMPLE.COM");
        assertThat(inquiry.getSubject()).isEqualTo("[RETURN] Order NRT-20260101123000-AB12CD34");
        assertThat(inquiry.getMessage())
                .contains("Ordered on: 2026-01-01")
                .contains("Product name/model: Sound insulation model X")
                .contains("Product code: SKU-100")
                .contains("Quantity: 2")
                .contains("Requested resolution: EXCHANGE")
                .endsWith("The product does not fit my vehicle.");
    }
}
