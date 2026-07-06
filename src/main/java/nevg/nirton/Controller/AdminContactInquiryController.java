package nevg.nirton.Controller;

import nevg.nirton.Service.AdminContactInquiryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/inquiries")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminContactInquiryController {

    private final AdminContactInquiryService service;

    public AdminContactInquiryController(AdminContactInquiryService service) {
        this.service = service;
    }

    @GetMapping
    public ModelAndView inquiries(@RequestParam(defaultValue = "0") int page) {
        ModelAndView result = new ModelAndView("admin-contact-inquiries");
        result.addObject("inquiries", service.getAll(page));
        return result;
    }

    @GetMapping("/{id}")
    public ModelAndView inquiry(@PathVariable Long id) {
        ModelAndView result = new ModelAndView("admin-contact-inquiry-details");
        result.addObject("inquiry", service.getAndMarkAsRead(id));
        return result;
    }
}
