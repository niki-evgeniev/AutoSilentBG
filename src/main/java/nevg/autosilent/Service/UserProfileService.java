package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.UserProfileDto;

public interface UserProfileService {

    UserProfileDto getProfile(String email);

    void updateProfile(String email, UserProfileDto profile);
}
