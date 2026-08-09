package nevg.autosilent.Controller;

import nevg.autosilent.Service.AdminIpAddressService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ip-addresses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIpAddressController {

    private final AdminIpAddressService adminIpAddressService;

    public AdminIpAddressController(AdminIpAddressService adminIpAddressService) {
        this.adminIpAddressService = adminIpAddressService;
    }

    @GetMapping
    public ModelAndView addresses(@RequestParam(defaultValue = "0") int page) {
        ModelAndView modelAndView = new ModelAndView("admin-ip-addresses");
        modelAndView.addObject("addresses", adminIpAddressService.getAll(page));
        modelAndView.addObject("visitStatistics", adminIpAddressService.getVisitStatistics());
        return modelAndView;
    }

    @PostMapping("/{id}/ban/permanent")
    public ModelAndView banPermanently(@PathVariable Long id,
                                       @RequestParam(defaultValue = "0") int page,
                                       RedirectAttributes redirectAttributes) {
        adminIpAddressService.banPermanently(id);
        redirectAttributes.addFlashAttribute("ipBanUpdated", true);
        return redirect(page);
    }

    @PostMapping("/{id}/ban/temporary")
    public ModelAndView banTemporarily(@PathVariable Long id,
                                       @RequestParam int days,
                                       @RequestParam(defaultValue = "0") int page,
                                       RedirectAttributes redirectAttributes) {
        adminIpAddressService.banForDays(id, days);
        redirectAttributes.addFlashAttribute("ipBanUpdated", true);
        return redirect(page);
    }

    @PostMapping("/{id}/unban")
    public ModelAndView removeBan(@PathVariable Long id,
                                  @RequestParam(defaultValue = "0") int page,
                                  RedirectAttributes redirectAttributes) {
        adminIpAddressService.removeBan(id);
        redirectAttributes.addFlashAttribute("ipBanUpdated", true);
        return redirect(page);
    }

    private ModelAndView redirect(int page) {
        return new ModelAndView("redirect:/admin/ip-addresses?page=" + Math.max(page, 0));
    }
}
