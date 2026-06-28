package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Entity.User;
import nevg.nirton.Models.Entity.UserRole;
import nevg.nirton.Models.Security.ShopUserDetails;
import nevg.nirton.Repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@NullMarked
public class ShopUserService implements UserDetailsService {
    private final UserRepository userRepository;

    public ShopUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .map(ShopUserService::mapUser)
                .orElseThrow(() -> new UsernameNotFoundException("User " + email + " not found"));
    }

    private static UserDetails mapUser(User user) {
        return new ShopUserDetails(
                user.getEmail(),
                user.getPassword(),
                user.getFirstName(),
                user.getRoles().stream().map(ShopUserService::map).toList()
        );
    }

    private static GrantedAuthority map(UserRole userRole) {
        return new SimpleGrantedAuthority("ROLE_" + userRole.getRoleType().name());
    }
}
