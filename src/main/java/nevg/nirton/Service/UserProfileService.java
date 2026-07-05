package nevg.nirton.Service;

import nevg.nirton.Models.Dto.UserProfileDto;

public interface UserProfileService {
    UserProfileDto getProfile(String email);
    void updateProfile(String email, UserProfileDto profile);
}
