package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.UserRegistrationDto;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Models.Entity.UserRole;
import nevg.autosilent.Models.Enums.RoleType;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Repository.UserRoleRepository;
import nevg.autosilent.Service.Exception.EmailAlreadyExistsException;
import nevg.autosilent.Service.UserRegistrationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

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
    public void addFirstAdminProfileAndAddRoles() {
        if (userRepository.count() == 0 && userRoleRepository.count() == 0) {
            User user = new User();
            user.setActivate(true);
            user.setEmail("info@carpmap.bg");
            String encode = passwordEncoder.encode("12345");
            user.setPassword(encode);
            user.setFirstName("Nikolay");
            user.setLastName("Ivanov");
            UserRole userRole = userRoleRepository.findByRoleType(RoleType.USER)
                    .orElseGet(() -> createRole(RoleType.USER));
            UserRole moderatorRole = userRoleRepository.findByRoleType(RoleType.MODERATOR)
                    .orElseGet(() -> createRole(RoleType.MODERATOR));
            UserRole adminRole = userRoleRepository.findByRoleType(RoleType.ADMIN)
                    .orElseGet(() -> createRole(RoleType.ADMIN));
            user.setRoles(Set.of(userRole, moderatorRole, adminRole));

            userRepository.save(user);
            System.out.println("Successful Add user : info@carpmap.bg and roles: USER, MODERATOR and ADMIN");
        }

    }

    @Override
    @Transactional
    public void register(UserRegistrationDto registration) {
        String normalizedEmail = registration.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User();
        user.setFirstName(registration.getFirstName().trim());
        user.setLastName(registration.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(normalizeOptional(registration.getPhoneNumber()));
        user.setPassword(passwordEncoder.encode(registration.getPassword()));
        user.setRegisterDate(LocalDateTime.now());
        user.setActivate(true);
        UserRole userRole = userRoleRepository.findByRoleType(RoleType.USER)
                .orElseGet(() -> createRole(RoleType.USER));
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
