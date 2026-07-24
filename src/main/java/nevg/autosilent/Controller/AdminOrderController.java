package nevg.autosilent.Controller;

import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Models.Security.ShopUserDetails;
import nevg.autosilent.Service.AdminOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public ModelAndView orders(@RequestParam(required = false) String search,
                               @RequestParam(required = false) OrderStatus status) {
        String normalizedSearch = search == null ? "" : search.trim();
        ModelAndView modelAndView = new ModelAndView("admin-orders");
        modelAndView.addObject("orders", adminOrderService.filterOrders(normalizedSearch, status));
        modelAndView.addObject("search", normalizedSearch);
        modelAndView.addObject("selectedStatus", status);
        modelAndView.addObject("statuses", OrderStatus.values());
        return modelAndView;
    }

    @GetMapping("/{id}")
    public ModelAndView order(@PathVariable Long id) {
        ModelAndView modelAndView = new ModelAndView("admin-order-details");
        modelAndView.addObject("order", adminOrderService.getOrder(id));
        modelAndView.addObject("statuses", OrderStatus.values());
        return modelAndView;
    }

    @PostMapping("/{id}/status")
    public ModelAndView updateStatus(@PathVariable Long id,
                                     @RequestParam OrderStatus status,
                                     @RequestParam(required = false) String note,
                                     @AuthenticationPrincipal ShopUserDetails currentUser,
                                     RedirectAttributes redirectAttributes) {
        adminOrderService.updateStatus(id, status, note, currentUser.getUsername());
        redirectAttributes.addFlashAttribute("statusUpdated", true);
        return new ModelAndView("redirect:/admin/orders/" + id);
    }
}
