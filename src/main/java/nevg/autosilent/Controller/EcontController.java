package nevg.autosilent.Controller;

import nevg.autosilent.Service.EcontDirectoryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import nevg.autosilent.Models.Entity.EcontCity;
import nevg.autosilent.Models.Entity.EcontOffice;

@Controller
public class EcontController {
    private final EcontDirectoryService directory;
    public EcontController(EcontDirectoryService directory) { this.directory = directory; }

    @GetMapping("/delivery/econt/cities") @ResponseBody
    public List<EcontCity> cities() { return directory.cities(); }

    @GetMapping("/delivery/econt/offices") @ResponseBody
    public List<EcontOffice> offices(@RequestParam Long cityId) { return directory.offices(cityId); }

    @GetMapping("/admin/settings/econt") @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView settings() {
        return new ModelAndView("admin-econt").addObject("sync", directory.status());
    }

    @PostMapping("/admin/settings/econt/refresh") @PreAuthorize("hasRole('ADMIN')")
    public String refresh(RedirectAttributes attributes) {
        attributes.addFlashAttribute("syncOk", directory.refresh());
        return "redirect:/admin/settings/econt";
    }
}
