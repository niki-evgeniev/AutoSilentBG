package nevg.autosilent.Service.Impl;

import nevg.autosilent.Models.Dto.UserProfileDto;
import nevg.autosilent.Models.Entity.User;
import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.Exception.IncorrectPasswordException;
import nevg.autosilent.Service.UserProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = findUser(email);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IncorrectPasswordException();
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setEditDate(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String email) {
        User user = findUser(email);
        UserProfileDto profile = new UserProfileDto();
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setEmail(user.getEmail());
        profile.setPhoneNumber(user.getPhoneNumber());
        return profile;
    }

    @Override
    @Transactional
    public void updateProfile(String email, UserProfileDto profile) {
        User user = findUser(email);
        user.setFirstName(profile.getFirstName().trim());
        user.setLastName(profile.getLastName().trim());
        user.setPhoneNumber(profile.getPhoneNumber() == null || profile.getPhoneNumber().isBlank()
                ? null : profile.getPhoneNumber().trim());
        user.setEditDate(LocalDateTime.now());
        userRepository.save(user);
        profile.setEmail(user.getEmail());
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Потребителят не е намерен."));
    }
}
