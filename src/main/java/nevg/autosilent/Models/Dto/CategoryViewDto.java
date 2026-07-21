package nevg.autosilent.Models.Dto;

import nevg.autosilent.Utility.ProductSlugGenerator;

public record CategoryViewDto(Long id, String name) {

    public String slug() {
        return ProductSlugGenerator.toSlug(name);
    }
}
