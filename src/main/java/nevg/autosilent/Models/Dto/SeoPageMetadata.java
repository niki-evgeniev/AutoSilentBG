package nevg.autosilent.Models.Dto;

public record SeoPageMetadata(
        String canonicalUrl,
        String bulgarianUrl,
        String englishUrl,
        String contentLanguage,
        String openGraphLocale,
        boolean englishContent
) {
    public boolean hasEnglishAlternate() {
        return englishUrl != null && !englishUrl.isBlank();
    }
}
