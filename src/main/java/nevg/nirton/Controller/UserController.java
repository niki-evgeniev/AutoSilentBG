package nevg.nirton.Controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UserController {

    @GetMapping("/user/sign_in")
    public ModelAndView login(){
        return new ModelAndView("login");
    }

    @GetMapping("/user/sign_up")
    public ModelAndView register(){
        return new ModelAndView("register");
    }
}
