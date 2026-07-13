package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.ContactInquiryDto;
import nevg.autosilent.Service.ClientIpResolver;
import nevg.autosilent.Service.ContactInquiryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock ContactInquiryService contactInquiryService;
    @Mock ClientIpResolver clientIpResolver;
    private ContactController controller;

    @BeforeEach
    void setUp() {
        controller = new ContactController(contactInquiryService, clientIpResolver);
    }

    @Test
    void contactReturnsEmptyForm() {
        ModelAndView result = controller.contact();

        assertThat(result.getViewName()).isEqualTo("contact");
        assertThat(result.getModel().get("inquiry")).isInstanceOf(ContactInquiryDto.class);
    }

    @Test
    void successReturnsDedicatedSuccessTemplate() {
        ModelAndView result = controller.success();

        assertThat(result.getViewName()).isEqualTo("contact-success");
        assertThat(result.getModel()).isEmpty();
        verifyNoInteractions(contactInquiryService, clientIpResolver);
    }

    @Test
    void submitStoresInquiryWithResolvedClientAddress() {
        ContactInquiryDto inquiry = inquiry();
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.8");
        ModelAndView result = controller.submit(inquiry,
                new BeanPropertyBindingResult(inquiry, "inquiry"), request);

        verify(contactInquiryService).create(inquiry, "203.0.113.8");
        assertThat(result.getViewName()).isEqualTo("redirect:/contact/success");
    }

    @Test
    void submitDoesNotStoreInvalidForm() {
        ContactInquiryDto inquiry = inquiry();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(inquiry, "inquiry");
        binding.rejectValue("name", "invalid");

        ModelAndView result = controller.submit(inquiry, binding,
                new MockHttpServletRequest());

        assertThat(result.getViewName()).isEqualTo("contact");
        assertThat(result.getModel().get("inquiry")).isSameAs(inquiry);
        verify(contactInquiryService, never()).create(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
        verifyNoInteractions(clientIpResolver);
    }

    private ContactInquiryDto inquiry() {
        ContactInquiryDto dto = new ContactInquiryDto();
        dto.setName("Ivan Ivanov");
        dto.setEmail("ivan@example.com");
        dto.setSubject("Question");
        dto.setMessage("This is my inquiry.");
        return dto;
    }
}
