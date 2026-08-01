package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.ProductReturnRequestDto;
import nevg.autosilent.Models.Enums.ReturnResolution;
import nevg.autosilent.Service.ClientIpResolver;
import nevg.autosilent.Service.ProductReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReturnControllerTest {

    @Mock ProductReturnService productReturnService;
    @Mock ClientIpResolver clientIpResolver;
    private ProductReturnController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductReturnController(productReturnService, clientIpResolver);
    }

    @Test
    void formReturnsEmptyReturnRequestAndCurrentDate() {
        ModelAndView result = controller.form();

        assertThat(result.getViewName()).isEqualTo("product-return");
        assertThat(result.getModel().get("returnRequest")).isInstanceOf(ProductReturnRequestDto.class);
        assertThat(result.getModel().get("today")).isEqualTo(LocalDate.now());
    }

    @Test
    void submitStoresValidReturnRequestWithResolvedClientAddress() {
        ProductReturnRequestDto request = validRequest();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        when(clientIpResolver.resolve(servletRequest)).thenReturn("203.0.113.9");

        ModelAndView result = controller.submit(request,
                new BeanPropertyBindingResult(request, "returnRequest"), servletRequest);

        verify(productReturnService).create(request, "203.0.113.9");
        assertThat(result.getViewName()).isEqualTo("redirect:/returns/success");
    }

    @Test
    void submitRedisplaysInvalidRequestWithoutStoringIt() {
        ProductReturnRequestDto request = validRequest();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(request, "returnRequest");
        binding.rejectValue("productName", "invalid");

        ModelAndView result = controller.submit(request, binding, new MockHttpServletRequest());

        assertThat(result.getViewName()).isEqualTo("product-return");
        assertThat(result.getModel().get("returnRequest")).isSameAs(request);
        verify(productReturnService, never()).create(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
        verifyNoInteractions(clientIpResolver);
    }

    @Test
    void successReturnsDedicatedTemplate() {
        ModelAndView result = controller.success();

        assertThat(result.getViewName()).isEqualTo("return-success");
        verifyNoInteractions(productReturnService, clientIpResolver);
    }

    private ProductReturnRequestDto validRequest() {
        ProductReturnRequestDto request = new ProductReturnRequestDto();
        request.setFirstName("Ivan");
        request.setLastName("Ivanov");
        request.setEmail("ivan@example.com");
        request.setPhone("+359 89 123 4567");
        request.setOrderNumber("NRT-20260101123000-AB12CD34");
        request.setOrderedOn(LocalDate.of(2026, 1, 1));
        request.setProductName("Sound insulation model X");
        request.setProductCode("SKU-100");
        request.setQuantity(2);
        request.setResolution(ReturnResolution.REFUND);
        request.setReason("The product does not fit my vehicle.");
        return request;
    }
}
