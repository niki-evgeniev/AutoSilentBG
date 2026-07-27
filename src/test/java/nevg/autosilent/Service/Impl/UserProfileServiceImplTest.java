package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.UserProfileDto;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.Exception.IncorrectPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void getProfileMapsUserFields() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        UserProfileDto result = service.getProfile("user@example.com");

        assertThat(result.getFirstName()).isEqualTo("Ivan");
        assertThat(result.getLastName()).isEqualTo("Ivanov");
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("0888123456");
    }

    @Test
    void updateProfileTrimsNamesPhoneAndKeepsPersistedEmailOnDto() {
        User user = user();
        UserProfileDto profile = new UserProfileDto();
        profile.setFirstName("  Petar  ");
        profile.setLastName("  Petrov  ");
        profile.setPhoneNumber("  0899123456  ");
        profile.setEmail("ignored@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        service.updateProfile("user@example.com", profile);

        assertThat(user.getFirstName()).isEqualTo("Petar");
        assertThat(user.getLastName()).isEqualTo("Petrov");
        assertThat(user.getPhoneNumber()).isEqualTo("0899123456");
        assertThat(user.getEditDate()).isNotNull();
        assertThat(profile.getEmail()).isEqualTo("user@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileStoresBlankPhoneAsNull() {
        User user = user();
        UserProfileDto profile = new UserProfileDto();
        profile.setFirstName("Petar");
        profile.setLastName("Petrov");
        profile.setPhoneNumber("   ");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        service.updateProfile("user@example.com", profile);

        assertThat(user.getPhoneNumber()).isNull();
    }

    @Test
    void getProfileThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void changePasswordVerifiesCurrentPasswordAndStoresEncodedNewPassword() {
        User user = user();
        user.setPassword("encoded-current");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-current")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

        service.changePassword("user@example.com", "current-password", "new-password");

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        assertThat(user.getEditDate()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        User user = user();
        user.setPassword("encoded-current");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-current")).thenReturn(false);

        assertThatThrownBy(() ->
                service.changePassword("user@example.com", "wrong-password", "new-password"))
                .isInstanceOf(IncorrectPasswordException.class);

        assertThat(user.getPassword()).isEqualTo("encoded-current");
    }

    private User user() {
        User user = new User();
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setEmail("user@example.com");
        user.setPhoneNumber("0888123456");
        return user;
    }
}
