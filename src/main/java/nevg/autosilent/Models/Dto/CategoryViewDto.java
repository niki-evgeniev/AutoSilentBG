package nevg.autosilent.Models.Dto;

import nevg.autosilent.Utility.ProductSlugGenerator;
import nevg.autosilent.Utility.CatalogCategoryUrlPolicy;

public record CategoryViewDto(Long id, String name) {

    public String slug() {
        return ProductSlugGenerator.toSlug(name);
    }

    public boolean rootCatalogCategory() {
        return CatalogCategoryUrlPolicy.isRootCatalogCategory(id, slug());
    }
}
