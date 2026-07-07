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
import org.springframework.security.crypto.password.PasswordEncoder;

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
    void duplicateEmailIsRejectedBeforeSaving() {
        User user = user(7L, "old@example.com", RoleType.USER);
        AdminUserEditDto request = editRequest();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(7L, request))
                .isInstanceOf(EmailAlreadyExistsException.class);
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
