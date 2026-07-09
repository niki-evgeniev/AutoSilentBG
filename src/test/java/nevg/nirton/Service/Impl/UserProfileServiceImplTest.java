package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.UserProfileDto;
import nevg.nirton.Models.Entity.User;
import nevg.nirton.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(userRepository);
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

    private User user() {
        User user = new User();
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setEmail("user@example.com");
        user.setPhoneNumber("0888123456");
        return user;
    }
}
