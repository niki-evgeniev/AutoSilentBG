package nevg.autosilent.Controller;


import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.UserRegistrationDto;
import nevg.autosilent.Models.Dto.UserProfileDto;
import nevg.autosilent.Models.Dto.ChangePasswordDto;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.Exception.EmailAlreadyExistsException;
import nevg.autosilent.Service.Exception.IncorrectPasswordException;
import nevg.autosilent.Service.UserRegistrationService;
import nevg.autosilent.Service.UserProfileService;
import nevg.autosilent.Service.UserOrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;

import java.util.Objects;

@Controller
public class UserController {

    private final UserRegistrationService userRegistrationService;
    private final UserProfileService userProfileService;
    private final UserOrderService userOrderService;

    public UserController(UserRegistrationService userRegistrationService,
                          UserProfileService userProfileService,
                          UserOrderService userOrderService) {
        this.userRegistrationService = userRegistrationService;
        this.userProfileService = userProfileService;
        this.userOrderService = userOrderService;
    }

    @GetMapping("/user/sign_in")
    public ModelAndView login() {
        return new ModelAndView("login");
    }

    @GetMapping("/user/forgot-password")
    public ModelAndView forgotPassword() {
        return new ModelAndView("forgot-password");
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
        return profileView(userProfileService.getProfile(currentUser.getUsername()), new ChangePasswordDto());
    }

    @GetMapping("/user/account")
    public ModelAndView account() {
        return new ModelAndView("account-dashboard");
    }

    @GetMapping("/user/orders")
    public ModelAndView userOrders(@AuthenticationPrincipal ShopUserDetails currentUser) {
        ModelAndView modelAndView = new ModelAndView("user-orders");
        modelAndView.addObject("orders", userOrderService.getOrders(currentUser.getUsername()));
        return modelAndView;
    }

    @GetMapping("/user/orders/{orderNumber}")
    public ModelAndView userOrder(@PathVariable String orderNumber,
                                  @AuthenticationPrincipal ShopUserDetails currentUser) {
        ModelAndView modelAndView = new ModelAndView("user-order-details");
        modelAndView.addObject(
                "order", userOrderService.getOrder(orderNumber, currentUser.getUsername()));
        return modelAndView;
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
            return profileView(profile, new ChangePasswordDto());
        }
        userProfileService.updateProfile(currentUser.getUsername(), profile);
        currentUser.setFirstName(profile.getFirstName().trim());
        redirectAttributes.addFlashAttribute("profileUpdated", true);
        return new ModelAndView("redirect:/user/profile");
    }

    @PostMapping("/user/profile/password")
    public ModelAndView changePassword(
            @Valid @ModelAttribute("changePassword") ChangePasswordDto changePassword,
            BindingResult bindingResult,
            @AuthenticationPrincipal ShopUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        if (!Objects.equals(changePassword.getNewPassword(), changePassword.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Паролите не съвпадат.");
        }
        if (!bindingResult.hasErrors()) {
            try {
                userProfileService.changePassword(
                        currentUser.getUsername(),
                        changePassword.getCurrentPassword(),
                        changePassword.getNewPassword());
            } catch (IncorrectPasswordException exception) {
                bindingResult.rejectValue(
                        "currentPassword", "password.incorrect", exception.getMessage());
            }
        }
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = profileView(
                    userProfileService.getProfile(currentUser.getUsername()), changePassword);
            modelAndView.addObject("openPasswordModal", true);
            return modelAndView;
        }
        redirectAttributes.addFlashAttribute("passwordChanged", true);
        return new ModelAndView("redirect:/user/profile");
    }

    private ModelAndView registrationView(UserRegistrationDto registration) {
        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registration", registration);
        return modelAndView;
    }

    private ModelAndView profileView(UserProfileDto profile, ChangePasswordDto changePassword) {
        ModelAndView modelAndView = new ModelAndView("profile");
        modelAndView.addObject("profile", profile);
        modelAndView.addObject("changePassword", changePassword);
        return modelAndView;
    }
}
