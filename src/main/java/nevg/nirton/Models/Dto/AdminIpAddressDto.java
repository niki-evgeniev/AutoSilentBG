package nevg.nirton.Models.Dto;

import java.time.LocalDateTime;

public record AdminIpAddressDto(
        Long id,
        String address,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen,
        Long countVisits,
        boolean banned,
        LocalDateTime bannedUntil,
        String userEmail
) {
}
