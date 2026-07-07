package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Entity.User;
import nevg.nirton.Models.Entity.UserRole;
import nevg.nirton.Models.Enums.RoleType;
import nevg.nirton.Models.Security.ShopUserDetails;
import nevg.nirton.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private ShopUserService shopUserService;

    @BeforeEach
    void setUp() {
        shopUserService = new ShopUserService(userRepository);
    }

    @Test
    void loadUserByUsernameMapsUserDetailsAndRoles() {
        User user = user(
                "owner@example.com",
                "encoded-password",
                "Ivan",
                role(RoleType.USER),
                role(RoleType.ADMIN)
        );
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));

        UserDetails result = shopUserService.loadUserByUsername("owner@example.com");

        assertThat(result).isInstanceOf(ShopUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("owner@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(((ShopUserDetails) result).getFirstName()).isEqualTo("Ivan");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsernameTrimsEmailBeforeSearching() {
        User user = user("user@example.com", "password", "Maria");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        UserDetails result = shopUserService.loadUserByUsername("  user@example.com  ");

        assertThat(result.getUsername()).isEqualTo("user@example.com");
        assertThat(result.getAuthorities()).isEmpty();
        verify(userRepository).findByEmailIgnoreCase("user@example.com");
    }

    @Test
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopUserService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User missing@example.com not found");
    }

    @Test
    void blockedUserIsMappedAsLockedAccount() {
        User user = user("blocked@example.com", "password", "Blocked", role(RoleType.USER));
        user.setBlocked(true);
        when(userRepository.findByEmailIgnoreCase("blocked@example.com")).thenReturn(Optional.of(user));

        UserDetails result = shopUserService.loadUserByUsername("blocked@example.com");

        assertThat(result.isAccountNonLocked()).isFalse();
    }

    private User user(String email, String password, String firstName, UserRole... roles) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setRoles(Set.of(roles));
        return user;
    }

    private UserRole role(RoleType roleType) {
        UserRole role = new UserRole();
        role.setRoleType(roleType);
        return role;
    }
}
