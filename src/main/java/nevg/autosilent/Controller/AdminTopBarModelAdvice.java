package nevg.autosilent.Controller;

import nevg.autosilent.Models.Dto.AdminVisitStatisticsDto;
import nevg.autosilent.Models.Enums.OrderStatus;
import nevg.autosilent.Repository.ContactInquiryRepository;
import nevg.autosilent.Repository.OrderRepository;
import nevg.autosilent.Service.AdminIpAddressService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
public class AdminTopBarModelAdvice {
    private final OrderRepository orderRepository;
    private final ContactInquiryRepository inquiryRepository;
    private final AdminIpAddressService ipAddressService;

    public AdminTopBarModelAdvice(OrderRepository orderRepository,
                                  ContactInquiryRepository inquiryRepository,
                                  AdminIpAddressService ipAddressService) {
        this.orderRepository = orderRepository;
        this.inquiryRepository = inquiryRepository;
        this.ipAddressService = ipAddressService;
    }

    @ModelAttribute("adminTopBar")
    public Map<String, Long> adminTopBar(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return Map.of();
        }
        AdminVisitStatisticsDto visits = ipAddressService.getVisitStatistics();
        return Map.of(
                "newOrders", orderRepository.countByOrderStatus(OrderStatus.NEW),
                "newInquiries", inquiryRepository.countByReadFalse(),
                "totalVisits", visits.totalVisits(),
                "todayVisitors", visits.todayVisits());
    }
}
