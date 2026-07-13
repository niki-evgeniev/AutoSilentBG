package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.AdminContactInquiryDto;
import org.springframework.data.domain.Page;

public interface AdminContactInquiryService {

    Page<AdminContactInquiryDto> getAll(int page);

    AdminContactInquiryDto getAndMarkAsRead(Long id);
}
