package nevg.nirton.Models.Dto;

import nevg.nirton.Models.Enums.RoleType;

import java.math.BigDecimal;

public record AdminUserSummaryDto(
        Long id, String email, String firstName, String lastName, String phoneNumber,
        RoleType role, BigDecimal discountPercent, boolean blocked
) {
}
