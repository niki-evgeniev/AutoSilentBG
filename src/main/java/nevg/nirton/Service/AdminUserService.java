package nevg.nirton.Service;

import nevg.nirton.Models.Dto.AdminUserEditDto;
import nevg.nirton.Models.Dto.AdminUserSummaryDto;

import java.util.List;

public interface AdminUserService {

    List<AdminUserSummaryDto> getAll();

    AdminUserEditDto getForEdit(Long id);

    void update(Long id, AdminUserEditDto request);
}
