package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.UserRegistrationDto;

public interface UserRegistrationService {

    void register(UserRegistrationDto registration);

    void addFirstAdminProfileAndAddRoles();
}
