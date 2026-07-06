package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.AdminContactInquiryDto;
import nevg.nirton.Models.Entity.ContactInquiry;
import nevg.nirton.Repository.ContactInquiryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminContactInquiryServiceImplTest {

    @Mock ContactInquiryRepository repository;

    @Test
    void getAndMarkAsReadMarksUnreadInquiry() {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setName("Ivan");
        inquiry.setEmail("ivan@example.com");
        inquiry.setSubject("Question");
        inquiry.setMessage("Message text");
        inquiry.setIpAddress("203.0.113.8");
        when(repository.findById(5L)).thenReturn(Optional.of(inquiry));

        AdminContactInquiryDto result = new AdminContactInquiryServiceImpl(repository).getAndMarkAsRead(5L);

        assertThat(inquiry.isRead()).isTrue();
        assertThat(result.read()).isTrue();
        assertThat(result.email()).isEqualTo("ivan@example.com");
        verify(repository).save(inquiry);
    }
}
