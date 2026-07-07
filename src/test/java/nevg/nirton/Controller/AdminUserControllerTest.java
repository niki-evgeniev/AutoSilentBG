package nevg.nirton.Controller;

import nevg.nirton.Models.Dto.AdminUserEditDto;
import nevg.nirton.Models.Dto.AdminUserSummaryDto;
import nevg.nirton.Models.Enums.RoleType;
import nevg.nirton.Service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock AdminUserService service;
    private AdminUserController controller;

    @BeforeEach
    void setUp() { controller = new AdminUserController(service); }

    @Test
    void controllerRequiresAdminRole() {
        assertThat(AdminUserController.class.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void usersReturnsSortedServiceResult() {
        var users = List.of(new AdminUserSummaryDto(1L, "a@example.com", "A", "B", null,
                RoleType.ADMIN, BigDecimal.ZERO, false));
        when(service.getAll()).thenReturn(users);

        var result = controller.users();

        assertThat(result.getViewName()).isEqualTo("admin-users");
        assertThat(result.getModel().get("users")).isSameAs(users);
    }

    @Test
    void detailsReturnsEditFormWithRoles() {
        AdminUserEditDto user = request();
        when(service.getForEdit(4L)).thenReturn(user);

        var result = controller.details(4L);

        assertThat(result.getViewName()).isEqualTo("admin-user-details");
        assertThat(result.getModel().get("user")).isSameAs(user);
        assertThat((RoleType[]) result.getModel().get("roles")).containsExactly(RoleType.values());
    }

    @Test
    void validUpdateDelegatesAndRedirects() {
        AdminUserEditDto user = request();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        var result = controller.update(4L, user,
                new BeanPropertyBindingResult(user, "user"), redirect);

        verify(service).update(4L, user);
        assertThat(result.getViewName()).isEqualTo("redirect:/admin/users/4");
        assertThat(redirect.getFlashAttributes()).containsKey("userUpdated");
    }

    @Test
    void invalidUpdateReturnsFormWithoutSaving() {
        AdminUserEditDto user = request();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(user, "user");
        binding.rejectValue("email", "invalid");

        var result = controller.update(4L, user, binding, new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("admin-user-details");
        verify(service, never()).update(4L, user);
    }

    private AdminUserEditDto request() {
        AdminUserEditDto dto = new AdminUserEditDto();
        dto.setFirstName("Ivan"); dto.setLastName("Ivanov"); dto.setEmail("ivan@example.com");
        dto.setRole(RoleType.USER); dto.setDiscountPercent(BigDecimal.ZERO); return dto;
    }
}
