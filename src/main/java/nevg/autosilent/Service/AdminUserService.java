package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.AdminUserEditDto;
import nevg.autosilent.Models.Dto.AdminUserSummaryDto;

import java.util.List;

public interface AdminUserService {

    List<AdminUserSummaryDto> getAll();

    AdminUserEditDto getForEdit(Long id);

    void update(Long id, AdminUserEditDto request);
}
