package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.UserRegistrationDto;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserRegistrationServiceImpl registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new UserRegistrationServiceImpl(
                userRepository,
                userRoleRepository,
                passwordEncoder
        );
    }

    @Test
    void registerNormalizesAndSavesActiveUserWithUserRole() {
        UserRegistrationDto registration = validRegistration();
        UserRole userRole = role(RoleType.USER);
        when(userRoleRepository.findByRoleType(RoleType.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        LocalDateTime beforeRegistration = LocalDateTime.now();

        registrationService.register(registration);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).existsByEmailIgnoreCase("user@example.com");
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Ivan");
        assertThat(saved.getLastName()).isEqualTo("Ivanov");
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getPhoneNumber()).isEqualTo("+359 888 123 456");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.isActivate()).isTrue();
        assertThat(saved.getRegisterDate()).isBetween(beforeRegistration, LocalDateTime.now());
        assertThat(saved.getRoles()).containsExactly(userRole);
    }

    @Test
    void registerStoresBlankPhoneNumberAsNull() {
        UserRegistrationDto registration = validRegistration();
        registration.setPhoneNumber("   ");
        when(userRoleRepository.findByRoleType(RoleType.USER)).thenReturn(Optional.of(role(RoleType.USER)));

        registrationService.register(registration);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPhoneNumber()).isNull();
    }

    @Test
    void registerRejectsExistingEmailBeforeEncodingOrSaving() {
        UserRegistrationDto registration = validRegistration();
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(registration))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(userRoleRepository, passwordEncoder);
    }

    @Test
    void registerCreatesUserRoleWhenItDoesNotExist() {
        UserRegistrationDto registration = validRegistration();
        when(userRoleRepository.findByRoleType(RoleType.USER)).thenReturn(Optional.empty());
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.register(registration);

        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleType()).isEqualTo(RoleType.USER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(roleCaptor.getValue());
    }

    @Test
    void registerTranslatesDatabaseUniqueConstraintViolation() {
        UserRegistrationDto registration = validRegistration();
        when(userRoleRepository.findByRoleType(RoleType.USER)).thenReturn(Optional.of(role(RoleType.USER)));
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        assertThatThrownBy(() -> registrationService.register(registration))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void addFirstAdminCreatesAdminAndAllRolesWhenDatabaseIsEmpty() {
        when(userRepository.count()).thenReturn(0L);
        when(userRoleRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("12345")).thenReturn("encoded-admin-password");
        when(userRoleRepository.findByRoleType(any(RoleType.class))).thenReturn(Optional.empty());
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.addFirstAdminProfileAndAddRoles();

        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository, org.mockito.Mockito.times(3)).save(roleCaptor.capture());
        assertThat(roleCaptor.getAllValues())
                .extracting(UserRole::getRoleType)
                .containsExactly(RoleType.USER, RoleType.MODERATOR, RoleType.ADMIN);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertThat(admin.getEmail()).isEqualTo("info@carpmap.bg");
        assertThat(admin.getPassword()).isEqualTo("encoded-admin-password");
        assertThat(admin.getFirstName()).isEqualTo("Nikolay");
        assertThat(admin.getLastName()).isEqualTo("Ivanov");
        assertThat(admin.isActivate()).isTrue();
        assertThat(admin.getRoles())
                .extracting(UserRole::getRoleType)
                .containsExactlyInAnyOrder(RoleType.USER, RoleType.MODERATOR, RoleType.ADMIN);
    }

    @Test
    void addFirstAdminDoesNothingWhenAUserAlreadyExists() {
        when(userRepository.count()).thenReturn(1L);

        registrationService.addFirstAdminProfileAndAddRoles();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(userRoleRepository, passwordEncoder);
    }

    @Test
    void addFirstAdminDoesNothingWhenRolesAlreadyExist() {
        when(userRepository.count()).thenReturn(0L);
        when(userRoleRepository.count()).thenReturn(1L);

        registrationService.addFirstAdminProfileAndAddRoles();

        verify(userRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    private UserRegistrationDto validRegistration() {
        UserRegistrationDto registration = new UserRegistrationDto();
        registration.setFirstName("  Ivan  ");
        registration.setLastName("  Ivanov  ");
        registration.setEmail("  USER@Example.COM  ");
        registration.setPhoneNumber("  +359 888 123 456  ");
        registration.setPassword("secret123");
        registration.setConfirmPassword("secret123");
        registration.setTermsAccepted(true);
        return registration;
    }

    private UserRole role(RoleType roleType) {
        UserRole role = new UserRole();
        role.setRoleType(roleType);
        return role;
    }
}
