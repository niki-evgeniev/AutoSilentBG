package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.AdminContactInquiryDto;
import nevg.autosilent.Service.AdminContactInquiryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminContactInquiryControllerTest {

    @Mock
    private AdminContactInquiryService service;

    private AdminContactInquiryController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminContactInquiryController(service);
    }

    @Test
    void controllerIsRestrictedToAdminAndModerator() {
        PreAuthorize authorization = AdminContactInquiryController.class.getAnnotation(PreAuthorize.class);
        RequestMapping mapping = AdminContactInquiryController.class.getAnnotation(RequestMapping.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasAnyRole('ADMIN', 'MODERATOR')");
        assertThat(mapping.value()).containsExactly("/admin/inquiries");
    }

    @Test
    void inquiriesReturnsRequestedPageInModel() {
        Page<AdminContactInquiryDto> inquiries = new PageImpl<>(List.of(inquiry(false)));
        when(service.getAll(2)).thenReturn(inquiries);

        ModelAndView result = controller.inquiries(2);

        assertThat(result.getViewName()).isEqualTo("admin-contact-inquiries");
        assertThat(result.getModel().get("inquiries")).isSameAs(inquiries);
        verify(service).getAll(2);
    }

    @Test
    void inquiriesSupportsEmptyResult() {
        Page<AdminContactInquiryDto> inquiries = Page.empty();
        when(service.getAll(0)).thenReturn(inquiries);

        ModelAndView result = controller.inquiries(0);

        assertThat(result.getModel().get("inquiries")).isSameAs(inquiries);
    }

    @Test
    void inquiryLoadsDetailsAndMarksItAsReadThroughService() {
        AdminContactInquiryDto inquiry = inquiry(true);
        when(service.getAndMarkAsRead(17L)).thenReturn(inquiry);

        ModelAndView result = controller.inquiry(17L);

        assertThat(result.getViewName()).isEqualTo("admin-contact-inquiry-details");
        assertThat(result.getModel().get("inquiry")).isSameAs(inquiry);
        verify(service).getAndMarkAsRead(17L);
    }

    private AdminContactInquiryDto inquiry(boolean read) {
        return new AdminContactInquiryDto(
                17L, "Ivan Ivanov", "ivan@example.com", "Product question",
                "I need more information.", "203.0.113.8",
                LocalDateTime.of(2026, 7, 7, 10, 30), read
        );
    }
}
