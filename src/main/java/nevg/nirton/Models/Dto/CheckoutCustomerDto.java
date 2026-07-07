package nevg.nirton.Models.Dto;

import java.math.BigDecimal;

public record CheckoutCustomerDto(
        String firstName,
        String lastName,
        String email,
        String phone,
        BigDecimal discountPercent
) {
    public CheckoutCustomerDto(String firstName, String lastName, String email, String phone) {
        this(firstName, lastName, email, phone, BigDecimal.ZERO);
    }
}
