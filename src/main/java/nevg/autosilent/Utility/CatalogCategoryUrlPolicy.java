package nevg.autosilent.Utility;

public final class CatalogCategoryUrlPolicy {

    public static final long ROOT_CATALOG_CATEGORY_ID = 1L;
    public static final String ROOT_CATALOG_CATEGORY_SLUG = "zvukoizolatsiya";

    private CatalogCategoryUrlPolicy() {
    }

    public static boolean isRootCatalogCategory(Long categoryId, String slug) {
        return categoryId != null
                && categoryId == ROOT_CATALOG_CATEGORY_ID
                && ROOT_CATALOG_CATEGORY_SLUG.equals(slug);
    }

    public static boolean isRootCatalogCategory(Long categoryId) {
        return categoryId != null && categoryId == ROOT_CATALOG_CATEGORY_ID;
    }
}
