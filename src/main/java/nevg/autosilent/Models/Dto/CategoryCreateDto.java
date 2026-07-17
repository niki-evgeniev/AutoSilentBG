package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryCreateDto {

    @NotBlank(message = "{validation.category.name.required}")
    @Size(min = 2, max = 80, message = "{validation.category.name.size}")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
