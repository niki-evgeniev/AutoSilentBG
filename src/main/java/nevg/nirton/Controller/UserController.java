package nevg.nirton.Controller;


import jakarta.validation.Valid;
import nevg.nirton.Models.Dto.UserRegistrationDto;
import nevg.nirton.Service.Exception.EmailAlreadyExistsException;
import nevg.nirton.Service.UserRegistrationService;
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

    public UserController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
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

    private ModelAndView registrationView(UserRegistrationDto registration) {
        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registration", registration);
        return modelAndView;
    }
}
