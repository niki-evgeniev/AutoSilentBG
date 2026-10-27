package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.*;

public record ProductQuestionDto(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Pattern(regexp = "^(?=(?:.*\\d){7,})[+]?[0-9 ()-]{7,20}$") String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 5, max = 4000) String question
) {}
