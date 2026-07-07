package nevg.nirton.Service;

import nevg.nirton.Models.Dto.AdminIpAddressDto;
import org.springframework.data.domain.Page;

public interface AdminIpAddressService {

    Page<AdminIpAddressDto> getAll(int page);

    void banPermanently(Long id);

    void banForDays(Long id, int days);

    void removeBan(Long id);
}
