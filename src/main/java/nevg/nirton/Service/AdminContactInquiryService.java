package nevg.nirton.Service;

import nevg.nirton.Models.Dto.AdminContactInquiryDto;
import org.springframework.data.domain.Page;

public interface AdminContactInquiryService {
    Page<AdminContactInquiryDto> getAll(int page);
    AdminContactInquiryDto getAndMarkAsRead(Long id);
}
