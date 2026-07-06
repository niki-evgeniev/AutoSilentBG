package nevg.nirton.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import nevg.nirton.Models.Dto.ContactInquiryDto;
import nevg.nirton.Service.ClientIpResolver;
import nevg.nirton.Service.ContactInquiryService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ContactController {

    private final ContactInquiryService contactInquiryService;
    private final ClientIpResolver clientIpResolver;

    public ContactController(ContactInquiryService contactInquiryService, ClientIpResolver clientIpResolver) {
        this.contactInquiryService = contactInquiryService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/contact")
    public ModelAndView contact() {
        return form(new ContactInquiryDto());
    }

    @GetMapping("/contact/success")
    public ModelAndView success() {
        return new ModelAndView("contact-success");
    }

    @PostMapping("/contact")
    public ModelAndView submit(@Valid @ModelAttribute("inquiry") ContactInquiryDto inquiry,
                               BindingResult bindingResult,
                               HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return form(inquiry);
        }
        contactInquiryService.create(inquiry, clientIpResolver.resolve(request));
        return new ModelAndView("redirect:/contact/success");
    }

    private ModelAndView form(ContactInquiryDto inquiry) {
        ModelAndView result = new ModelAndView("contact");
        result.addObject("inquiry", inquiry);
        return result;
    }
}
