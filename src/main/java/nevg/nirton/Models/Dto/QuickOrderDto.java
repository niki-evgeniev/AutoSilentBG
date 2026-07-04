package nevg.nirton.Models.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QuickOrderDto(
        @NotNull Long productId,
        @Min(1) int quantity,
        @NotBlank @Size(max = 201) String names,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^(?=(?:.*\\d){7,})[+]?[0-9 ()-]{7,20}$") String phone
) {
}
