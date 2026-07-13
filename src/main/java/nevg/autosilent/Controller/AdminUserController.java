package nevg.autosilent.Controller;

import jakarta.validation.Valid;
import nevg.autosilent.Models.Dto.AdminUserEditDto;
import nevg.autosilent.Models.Enums.RoleType;
import nevg.autosilent.Service.AdminUserService;
import nevg.autosilent.Service.Exception.EmailAlreadyExistsException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public ModelAndView users() {
        ModelAndView result = new ModelAndView("admin-users");
        result.addObject("users", service.getAll());
        return result;
    }

    @GetMapping("/{id}")
    public ModelAndView details(@PathVariable Long id) {
        return form(id, service.getForEdit(id));
    }

    @PostMapping("/{id}")
    public ModelAndView update(@PathVariable Long id,
                               @Valid @ModelAttribute("user") AdminUserEditDto user,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) return form(id, user);
        try {
            service.update(id, user);
        } catch (EmailAlreadyExistsException exception) {
            bindingResult.rejectValue("email", "email.exists", exception.getMessage());
            return form(id, user);
        }
        redirectAttributes.addFlashAttribute("userUpdated", true);
        return new ModelAndView("redirect:/admin/users/" + id);
    }

    private ModelAndView form(Long id, AdminUserEditDto user) {
        ModelAndView result = new ModelAndView("admin-user-details");
        result.addObject("user", user);
        result.addObject("userId", id);
        result.addObject("roles", RoleType.values());
        return result;
    }
}
