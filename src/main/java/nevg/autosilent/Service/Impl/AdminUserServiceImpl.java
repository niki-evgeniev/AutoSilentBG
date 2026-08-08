package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.AdminUserEditDto;
import nevg.autosilent.Models.Dto.AdminUserSummaryDto;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Models.Entity.UserRole;
import nevg.autosilent.Models.Enums.RoleType;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Repository.UserRoleRepository;
import nevg.autosilent.Service.AdminUserService;
import nevg.autosilent.Service.Exception.EmailAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final int PAGE_SIZE = 10;
    private final UserRepository userRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(UserRepository userRepository, UserRoleRepository roleRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDto> getAll(String query, int page) {
        String normalizedQuery = normalizeSearchQuery(query);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Order.asc("email").ignoreCase()));
        Page<User> users = normalizedQuery.isEmpty()
                ? userRepository.findAll(pageRequest)
                : userRepository.searchByEmailOrName(normalizedQuery, pageRequest);
        List<AdminUserSummaryDto> content = users.stream()
                .map(user -> new AdminUserSummaryDto(user.getId(), user.getEmail(), user.getFirstName(),
                        user.getLastName(), user.getPhoneNumber(), highestRole(user),
                        discount(user), user.isBlocked()))
                .sorted(Comparator.comparingInt((AdminUserSummaryDto user) -> roleOrder(user.role()))
                        .thenComparing(AdminUserSummaryDto::email, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new PageImpl<>(content, pageRequest, users.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserEditDto getForEdit(Long id) {
        User user = findUser(id);
        AdminUserEditDto dto = new AdminUserEditDto();
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(highestRole(user));
        dto.setDiscountPercent(discount(user));
        dto.setBlocked(user.isBlocked());
        return dto;
    }

    @Override
    @Transactional
    public void update(Long id, AdminUserEditDto request) {
        User user = findUser(id);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new EmailAlreadyExistsException();
        }
        UserRole role = roleRepository.findByRoleType(request.getRole())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Role not found."));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPhoneNumber(normalizeOptional(request.getPhoneNumber()));
        user.getRoles().clear();
        user.getRoles().add(role);
        user.setDiscountPercent(request.getDiscountPercent().setScale(2));
        user.setBlocked(request.isBlocked());
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        userRepository.save(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private RoleType highestRole(User user) {
        if (user.getRoles().stream().anyMatch(role -> role.getRoleType() == RoleType.ADMIN)) return RoleType.ADMIN;
        if (user.getRoles().stream().anyMatch(role -> role.getRoleType() == RoleType.MODERATOR))
            return RoleType.MODERATOR;
        return RoleType.USER;
    }

    private int roleOrder(RoleType role) {
        return switch (role) {
            case ADMIN -> 0;
            case MODERATOR -> 1;
            case USER -> 2;
        };
    }

    private BigDecimal discount(User user) {
        return user.getDiscountPercent() == null ? BigDecimal.ZERO : user.getDiscountPercent();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeSearchQuery(String query) {
        return query == null ? "" : query.trim().replaceAll("\\s+", " ");
    }
}
