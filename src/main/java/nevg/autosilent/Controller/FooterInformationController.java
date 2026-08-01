package nevg.autosilent.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FooterInformationController {

    @GetMapping("/delivery")
    public ModelAndView delivery() {
        return new ModelAndView("delivery");
    }

    @GetMapping("/payment")
    public ModelAndView payment() {
        return new ModelAndView("payment");
    }

    @GetMapping("/privacy-policy")
    public ModelAndView privacyPolicy() {
        return new ModelAndView("privacy-policy");
    }
}
