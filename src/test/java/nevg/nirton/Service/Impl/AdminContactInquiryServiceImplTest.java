package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.AdminContactInquiryDto;
import nevg.nirton.Models.Entity.ContactInquiry;
import nevg.nirton.Repository.ContactInquiryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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

    @Test
    void getAllUsesThirtyItemPageAndMapsInquiry() {
        ContactInquiry inquiry = inquiry();
        when(repository.findAllByOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inquiry)));

        var result = new AdminContactInquiryServiceImpl(repository).getAll(-4);

        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.name()).isEqualTo("Ivan");
            assertThat(dto.subject()).isEqualTo("Question");
        });
        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrderByCreatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(30);
    }

    @Test
    void alreadyReadInquiryIsNotSavedAgain() {
        ContactInquiry inquiry = inquiry();
        inquiry.setRead(true);
        when(repository.findById(5L)).thenReturn(Optional.of(inquiry));

        var result = new AdminContactInquiryServiceImpl(repository).getAndMarkAsRead(5L);

        assertThat(result.read()).isTrue();
        verify(repository, never()).save(inquiry);
    }

    @Test
    void missingInquiryReturnsNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AdminContactInquiryServiceImpl(repository).getAndMarkAsRead(404L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private ContactInquiry inquiry() {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setName("Ivan");
        inquiry.setEmail("ivan@example.com");
        inquiry.setSubject("Question");
        inquiry.setMessage("Message text");
        inquiry.setIpAddress("203.0.113.8");
        return inquiry;
    }
}
