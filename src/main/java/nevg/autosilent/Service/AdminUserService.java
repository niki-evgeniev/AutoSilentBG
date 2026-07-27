package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.AdminUserEditDto;
import nevg.autosilent.Models.Dto.AdminUserSummaryDto;

import org.springframework.data.domain.Page;

public interface AdminUserService {

    Page<AdminUserSummaryDto> getAll(String query, int page);

    AdminUserEditDto getForEdit(Long id);

    void update(Long id, AdminUserEditDto request);
}
