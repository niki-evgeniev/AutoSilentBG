package nevg.nirton.Service.Impl;

import nevg.nirton.Models.Dto.UserRegistrationDto;
import nevg.nirton.Models.Entity.User;
import nevg.nirton.Models.Entity.UserRole;
import nevg.nirton.Models.Enums.RoleType;
import nevg.nirton.Repository.UserRepository;
import nevg.nirton.Repository.UserRoleRepository;
import nevg.nirton.Service.Exception.EmailAlreadyExistsException;
import nevg.nirton.Service.UserRegistrationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class UserRegistrationServiceImpl implements UserRegistrationService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationServiceImpl(UserRepository userRepository,
                                       UserRoleRepository userRoleRepository,
                                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void register(UserRegistrationDto registration) {
        String normalizedEmail = registration.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        UserRole userRole = userRoleRepository.findByRoleType(RoleType.USER)
                .orElseGet(() -> createRole(RoleType.USER));

        User user = new User();
        user.setFirstName(registration.getFirstName().trim());
        user.setLastName(registration.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(normalizeOptional(registration.getPhoneNumber()));
        user.setPassword(passwordEncoder.encode(registration.getPassword()));
        user.setRegisterDate(LocalDateTime.now());
        user.setEditDate(LocalDateTime.now());
        user.setActivate(true);
        user.getRoles().add(userRole);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // The unique database constraint also protects against two concurrent requests.
            throw new EmailAlreadyExistsException();
        }
    }

    private UserRole createRole(RoleType roleType) {
        UserRole role = new UserRole();
        role.setRoleType(roleType);
        return userRoleRepository.save(role);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
