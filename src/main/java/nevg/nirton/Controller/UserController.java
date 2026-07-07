package nevg.nirton.Controller;


import jakarta.validation.Valid;
import nevg.nirton.Models.Dto.UserRegistrationDto;
import nevg.nirton.Models.Dto.UserProfileDto;
import nevg.nirton.Models.Security.ShopUserDetails;
import nevg.nirton.Service.Exception.EmailAlreadyExistsException;
import nevg.nirton.Service.UserRegistrationService;
import nevg.nirton.Service.UserProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;

import java.util.Objects;

@Controller
public class UserController {

    private final UserRegistrationService userRegistrationService;
    private final UserProfileService userProfileService;

    public UserController(UserRegistrationService userRegistrationService, UserProfileService userProfileService) {
        this.userRegistrationService = userRegistrationService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/user/sign_in")
    public ModelAndView login(){
        return new ModelAndView("login");
    }

    @GetMapping("/user/sign_up")
    public ModelAndView register() {
        return registrationView(new UserRegistrationDto());
    }

    @PostMapping("/user/sign_up")
    public ModelAndView register(@Valid @ModelAttribute("registration") UserRegistrationDto registration,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (!Objects.equals(registration.getPassword(), registration.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Паролите не съвпадат.");
        }

        if (bindingResult.hasErrors()) {
            return registrationView(registration);
        }

        try {
            userRegistrationService.register(registration);
        } catch (EmailAlreadyExistsException exception) {
            bindingResult.rejectValue("email", "email.exists", exception.getMessage());
            return registrationView(registration);
        }

        redirectAttributes.addFlashAttribute("registrationSuccess", true);
        return new ModelAndView("redirect:/user/sign_in");
    }

    @GetMapping("/user/profile")
    public ModelAndView profile(@AuthenticationPrincipal ShopUserDetails currentUser) {
        return profileView(userProfileService.getProfile(currentUser.getUsername()));
    }

    @GetMapping("/user/account")
    public ModelAndView account() {
        return new ModelAndView("account-dashboard");
    }

    @GetMapping("/user/orders")
    public ModelAndView userOrders() {
        return new ModelAndView("user-orders");
    }

    @GetMapping("/user/addresses")
    public ModelAndView userAddresses() {
        return new ModelAndView("user-addresses");
    }

    @PostMapping("/user/profile")
    public ModelAndView updateProfile(@Valid @ModelAttribute("profile") UserProfileDto profile,
                                      BindingResult bindingResult,
                                      @AuthenticationPrincipal ShopUserDetails currentUser,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            profile.setEmail(currentUser.getUsername());
            return profileView(profile);
        }
        userProfileService.updateProfile(currentUser.getUsername(), profile);
        currentUser.setFirstName(profile.getFirstName().trim());
        redirectAttributes.addFlashAttribute("profileUpdated", true);
        return new ModelAndView("redirect:/user/profile");
    }

    private ModelAndView registrationView(UserRegistrationDto registration) {
        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registration", registration);
        return modelAndView;
    }

    private ModelAndView profileView(UserProfileDto profile) {
        ModelAndView modelAndView = new ModelAndView("profile");
        modelAndView.addObject("profile", profile);
        return modelAndView;
    }
}
