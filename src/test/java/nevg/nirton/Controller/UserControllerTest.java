package nevg.nirton.Controller;

import nevg.nirton.Models.Dto.UserRegistrationDto;
import nevg.nirton.Service.Exception.EmailAlreadyExistsException;
import nevg.nirton.Service.UserRegistrationService;
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

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRegistrationService registrationService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(registrationService);
    }

    @Test
    void loginReturnsLoginView() {
        assertThat(userController.login().getViewName()).isEqualTo("login");
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

    private UserRegistrationDto registration(String password, String confirmPassword) {
        UserRegistrationDto registration = new UserRegistrationDto();
        registration.setPassword(password);
        registration.setConfirmPassword(confirmPassword);
        return registration;
    }

    private BindingResult bindingResult(UserRegistrationDto registration) {
        return new BeanPropertyBindingResult(registration, "registration");
    }
}
