package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.AdminIpAddressDto;
import nevg.autosilent.Models.Dto.AdminVisitStatisticsDto;
import org.springframework.data.domain.Page;

public interface AdminIpAddressService {

    Page<AdminIpAddressDto> getAll(int page);

    AdminVisitStatisticsDto getVisitStatistics();

    void banPermanently(Long id);

    void banForDays(Long id, int days);

    void removeBan(Long id);
}
