package nevg.autosilent.Models.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CartOrderDto(
        @NotEmpty List<@Valid CartItemOrderDto> items,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^(?=(?:.*\\d){7,})[+]?[0-9 ()-]{7,20}$") String phone,
        @Size(max = 2000) String customerNote
) {
}
