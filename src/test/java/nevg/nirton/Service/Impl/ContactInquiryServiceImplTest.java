package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.ContactInquiryDto;
import nevg.nirton.Models.Entity.ContactInquiry;
import nevg.nirton.Repository.ContactInquiryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContactInquiryServiceImplTest {

    @Mock ContactInquiryRepository repository;

    @Test
    void createTrimsAndStoresInquiry() {
        ContactInquiryDto dto = new ContactInquiryDto();
        dto.setName("  Ivan Ivanov  ");
        dto.setEmail("  IVAN@EXAMPLE.COM  ");
        dto.setSubject("  Product question  ");
        dto.setMessage("  I need more information.  ");

        new ContactInquiryServiceImpl(repository).create(dto, "203.0.113.8");

        ArgumentCaptor<ContactInquiry> captor = ArgumentCaptor.forClass(ContactInquiry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ivan Ivanov");
        assertThat(captor.getValue().getEmail()).isEqualTo("ivan@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Product question");
        assertThat(captor.getValue().getMessage()).isEqualTo("I need more information.");
        assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.8");
    }
}
