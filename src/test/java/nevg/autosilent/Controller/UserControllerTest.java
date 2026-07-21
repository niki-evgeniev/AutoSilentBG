package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.UserRegistrationDto;
import nevg.autosilent.Models.Dto.UserProfileDto;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.Exception.EmailAlreadyExistsException;
import nevg.autosilent.Service.UserRegistrationService;
import nevg.autosilent.Service.UserProfileService;
import nevg.autosilent.Service.UserOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRegistrationService registrationService;
    @Mock
    private UserProfileService profileService;
    @Mock
    private UserOrderService userOrderService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(registrationService, profileService, userOrderService);
    }

    @Test
    void loginReturnsLoginView() {
        assertThat(userController.login().getViewName()).isEqualTo("login");
    }

    @Test
    void forgotPasswordReturnsSupportPage() {
        assertThat(userController.forgotPassword().getViewName()).isEqualTo("forgot-password");
    }

    @Test
    void registerFormReturnsEmptyRegistrationModel() {
        ModelAndView result = userController.register();

        assertThat(result.getViewName()).isEqualTo("register");
        assertThat(result.getModel().get("registration")).isInstanceOf(UserRegistrationDto.class);
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        UserRegistrationDto registration = registration("password1", "password2");
        BindingResult bindingResult = bindingResult(registration);

        ModelAndView result = userController.register(
                registration, bindingResult, new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("register");
        assertThat(result.getModel().get("registration")).isSameAs(registration);
        assertThat(bindingResult.getFieldError("confirmPassword"))
                .isNotNull()
                .extracting(error -> error.getCode())
                .isEqualTo("password.mismatch");
        verify(registrationService, never()).register(registration);
    }

    @Test
    void registerAddsEmailErrorWhenEmailAlreadyExists() {
        UserRegistrationDto registration = registration("password1", "password1");
        BindingResult bindingResult = bindingResult(registration);
        doThrow(new EmailAlreadyExistsException()).when(registrationService).register(registration);

        ModelAndView result = userController.register(
                registration, bindingResult, new RedirectAttributesModelMap());

        assertThat(result.getViewName()).isEqualTo("register");
        assertThat(bindingResult.getFieldError("email"))
                .isNotNull()
                .extracting(error -> error.getCode())
                .isEqualTo("email.exists");
    }

    @Test
    void registerRedirectsAndAddsFlashMessageOnSuccess() {
        UserRegistrationDto registration = registration("password1", "password1");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView result = userController.register(
                registration, bindingResult(registration), redirectAttributes);

        verify(registrationService).register(registration);
        assertThat(result.getViewName()).isEqualTo("redirect:/user/sign_in");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("registrationSuccess");
    }

    @Test
    void profileReturnsCurrentUsersData() {
        UserProfileDto profile = new UserProfileDto();
        when(profileService.getProfile("user@example.com")).thenReturn(profile);

        ModelAndView result = userController.profile(currentUser());

        assertThat(result.getViewName()).isEqualTo("profile");
        assertThat(result.getModel().get("profile")).isSameAs(profile);
    }

    @Test
    void updateProfileSavesAndRedirects() {
        UserProfileDto profile = new UserProfileDto();
        profile.setFirstName("Ivan");
        profile.setLastName("Ivanov");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView result = userController.updateProfile(profile,
                new BeanPropertyBindingResult(profile, "profile"), currentUser(), redirectAttributes);

        verify(profileService).updateProfile("user@example.com", profile);
        assertThat(result.getViewName()).isEqualTo("redirect:/user/profile");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("profileUpdated");
    }

    @Test
    void accountReturnsDashboard() {
        assertThat(userController.account().getViewName()).isEqualTo("account-dashboard");
    }

    @Test
    void userOrdersReturnsOrdersView() {
        when(userOrderService.getOrders("user@example.com")).thenReturn(List.of());

        ModelAndView result = userController.userOrders(currentUser());

        assertThat(result.getViewName()).isEqualTo("user-orders");
        assertThat(result.getModel()).containsEntry("orders", List.of());
        verify(userOrderService).getOrders("user@example.com");
    }

    @Test
    void userAddressesReturnsAddressesView() {
        assertThat(userController.userAddresses().getViewName()).isEqualTo("user-addresses");
    }

    private UserRegistrationDto registration(String password, String confirmPassword) {
        UserRegistrationDto registration = new UserRegistrationDto();
        registration.setPassword(password);
        registration.setConfirmPassword(confirmPassword);
        return registration;
    }

    private BindingResult bindingResult(UserRegistrationDto registration) {
        return new BeanPropertyBindingResult(registration, "registration");
    }

    private ShopUserDetails currentUser() {
        return new ShopUserDetails("user@example.com", "password", "Ivan", List.of());
    }
}
