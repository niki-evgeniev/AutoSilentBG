package nevg.nirton.Models.Dto;

import java.time.LocalDateTime;

public record AdminContactInquiryDto(
        Long id,
        String name,
        String email,
        String subject,
        String message,
        String ipAddress,
        LocalDateTime createdAt,
        boolean read
) {
}
