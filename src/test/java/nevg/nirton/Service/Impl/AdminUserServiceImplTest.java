package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.AdminUserEditDto;
import nevg.nirton.Models.Entity.User;
import nevg.nirton.Models.Entity.UserRole;
import nevg.nirton.Models.Enums.RoleType;
import nevg.nirton.Repository.UserRepository;
import nevg.nirton.Repository.UserRoleRepository;
import nevg.nirton.Service.Exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserRoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    private AdminUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminUserServiceImpl(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void getAllSortsAdminsThenModeratorsThenUsers() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(3L, "user@example.com", RoleType.USER),
                user(1L, "admin@example.com", RoleType.ADMIN),
                user(2L, "moderator@example.com", RoleType.MODERATOR)));

        var result = service.getAll();

        assertThat(result).extracting(dto -> dto.role())
                .containsExactly(RoleType.ADMIN, RoleType.MODERATOR, RoleType.USER);
    }

    @Test
    void getAllSortsEmailsCaseInsensitivelyWithinSameRoleAndMapsFields() {
        User beta = user(2L, "beta@example.com", RoleType.USER);
        beta.setFirstName("Beta");
        beta.setLastName("User");
        beta.setPhoneNumber("0888000002");
        beta.setDiscountPercent(null);
        beta.setBlocked(true);
        User alpha = user(1L, "Alpha@example.com", RoleType.USER);
        alpha.setFirstName("Alpha");
        alpha.setLastName("User");
        alpha.setPhoneNumber("0888000001");
        alpha.setDiscountPercent(new BigDecimal("7.50"));
        when(userRepository.findAll()).thenReturn(List.of(beta, alpha));

        var result = service.getAll();

        assertThat(result).extracting(dto -> dto.email())
                .containsExactly("Alpha@example.com", "beta@example.com");
        assertThat(result.get(0).firstName()).isEqualTo("Alpha");
        assertThat(result.get(0).lastName()).isEqualTo("User");
        assertThat(result.get(0).phoneNumber()).isEqualTo("0888000001");
        assertThat(result.get(0).discountPercent()).isEqualByComparingTo("7.50");
        assertThat(result.get(0).blocked()).isFalse();
        assertThat(result.get(1).discountPercent()).isEqualByComparingTo("0.00");
        assertThat(result.get(1).blocked()).isTrue();
    }

    @Test
    void getAllUsesHighestRoleWhenUserHasMultipleRoles() {
        User user = user(7L, "user@example.com", RoleType.USER);
        user.getRoles().add(role(RoleType.ADMIN));
        when(userRepository.findAll()).thenReturn(List.of(user));

        var result = service.getAll();

        assertThat(result).singleElement().satisfies(dto -> assertThat(dto.role()).isEqualTo(RoleType.ADMIN));
    }

    @Test
    void getForEditMapsUserFieldsAndHighestRole() {
        User user = user(7L, "user@example.com", RoleType.USER);
        user.getRoles().add(role(RoleType.MODERATOR));
        user.setPhoneNumber("0888123456");
        user.setDiscountPercent(null);
        user.setBlocked(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        AdminUserEditDto result = service.getForEdit(7L);

        assertThat(result.getFirstName()).isEqualTo("First");
        assertThat(result.getLastName()).isEqualTo("Last");
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("0888123456");
        assertThat(result.getRole()).isEqualTo(RoleType.MODERATOR);
        assertThat(result.getDiscountPercent()).isEqualByComparingTo("0.00");
        assertThat(result.isBlocked()).isTrue();
    }

    @Test
    void getForEditThrowsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForEdit(404L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateChangesProfileRoleDiscountBlockAndPassword() {
        User user = user(7L, "old@example.com", RoleType.USER);
        var managedRolesCollection = user.getRoles();
        UserRole moderator = role(RoleType.MODERATOR);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleType(RoleType.MODERATOR)).thenReturn(Optional.of(moderator));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
        AdminUserEditDto request = editRequest();

        service.update(7L, request);

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("Name");
        assertThat(user.getPhoneNumber()).isEqualTo("0888123456");
        assertThat(user.getRoles()).isSameAs(managedRolesCollection);
        assertThat(user.getRoles()).containsExactly(moderator);
        assertThat(user.getDiscountPercent()).isEqualByComparingTo("20.00");
        assertThat(user.isBlocked()).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(userRepository).save(user);
    }

    @Test
    void updateWithoutPasswordKeepsCurrentPassword() {
        User user = user(7L, "old@example.com", RoleType.USER);
        user.setPassword("existing");
        AdminUserEditDto request = editRequest();
        request.setNewPassword("");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleType(RoleType.MODERATOR)).thenReturn(Optional.of(role(RoleType.MODERATOR)));

        service.update(7L, request);

        assertThat(user.getPassword()).isEqualTo("existing");
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void updateStoresBlankPhoneAsNullAndScalesDiscount() {
        User user = user(7L, "old@example.com", RoleType.USER);
        AdminUserEditDto request = editRequest();
        request.setFirstName("  New  ");
        request.setLastName("  Name  ");
        request.setPhoneNumber("   ");
        request.setDiscountPercent(new BigDecimal("20"));
        request.setNewPassword(null);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleType(RoleType.MODERATOR)).thenReturn(Optional.of(role(RoleType.MODERATOR)));

        service.update(7L, request);

        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("Name");
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getDiscountPercent()).isEqualByComparingTo("20.00");
        assertThat(user.getDiscountPercent().scale()).isEqualTo(2);
    }

    @Test
    void duplicateEmailIsRejectedBeforeSaving() {
        User user = user(7L, "old@example.com", RoleType.USER);
        AdminUserEditDto request = editRequest();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(7L, request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateThrowsNotFoundWhenUserDoesNotExist() {
        AdminUserEditDto request = editRequest();
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateThrowsInternalServerErrorWhenRoleDoesNotExist() {
        User user = user(7L, "old@example.com", RoleType.USER);
        AdminUserEditDto request = editRequest();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleType(RoleType.MODERATOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(7L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private AdminUserEditDto editRequest() {
        AdminUserEditDto dto = new AdminUserEditDto();
        dto.setFirstName("New"); dto.setLastName("Name"); dto.setEmail("NEW@example.com");
        dto.setPhoneNumber("0888123456"); dto.setRole(RoleType.MODERATOR);
        dto.setDiscountPercent(new BigDecimal("20")); dto.setBlocked(true); dto.setNewPassword("new-password");
        return dto;
    }

    private User user(Long id, String email, RoleType roleType) {
        User user = new User(); user.setId(id); user.setEmail(email); user.setFirstName("First"); user.setLastName("Last");
        user.getRoles().add(role(roleType)); return user;
    }

    private UserRole role(RoleType type) {
        UserRole role = new UserRole(); role.setRoleType(type); return role;
    }
}
