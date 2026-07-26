package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.AdminIpAddressDto;
import nevg.autosilent.Service.AdminIpAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminIpAddressControllerTest {

    @Mock
    private AdminIpAddressService service;

    private AdminIpAddressController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminIpAddressController(service);
    }

    @Test
    void controllerIsRestrictedToAdmin() {
        PreAuthorize authorization = AdminIpAddressController.class.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void addressesReturnsRequestedPage() {
        Page<AdminIpAddressDto> addresses = new PageImpl<>(List.of());
        when(service.getAll(2)).thenReturn(addresses);

        ModelAndView result = controller.addresses(2);

        assertThat(result.getViewName()).isEqualTo("admin-ip-addresses");
        assertThat(result.getModel().get("addresses")).isSameAs(addresses);
    }

    @Test
    void temporaryBanDelegatesToService() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView result = controller.banTemporarily(8L, 30, 2, redirectAttributes);

        verify(service).banForDays(8L, 30);
        assertThat(result.getViewName()).isEqualTo("redirect:/admin/ip-addresses?page=2");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("ipBanUpdated");
    }

    @Test
    void permanentBanAndUnbanDelegateToService() {
        controller.banPermanently(8L, 0, new RedirectAttributesModelMap());
        controller.removeBan(8L, 0, new RedirectAttributesModelMap());

        verify(service).banPermanently(8L);
        verify(service).removeBan(8L);
    }
}
