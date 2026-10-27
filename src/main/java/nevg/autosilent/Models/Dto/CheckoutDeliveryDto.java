package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.*;

public record CheckoutDeliveryDto(
        @NotNull Long cityId,
        @NotBlank @Pattern(regexp = "OFFICE|ADDRESS") String type,
        Long officeId,
        @Size(max = 255) String address
) {}
