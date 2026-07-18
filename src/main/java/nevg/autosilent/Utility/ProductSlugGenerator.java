package nevg.autosilent.Utility;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

public final class ProductSlugGenerator {

    private static final int MAX_LENGTH = 180;
    private static final Map<Character, String> CYRILLIC = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"),
            Map.entry('г', "g"), Map.entry('д', "d"), Map.entry('е', "e"),
            Map.entry('ж', "zh"), Map.entry('з', "z"), Map.entry('и', "i"),
            Map.entry('й', "y"), Map.entry('к', "k"), Map.entry('л', "l"),
            Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
            Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"),
            Map.entry('т', "t"), Map.entry('у', "u"), Map.entry('ф', "f"),
            Map.entry('х', "h"), Map.entry('ц', "ts"), Map.entry('ч', "ch"),
            Map.entry('ш', "sh"), Map.entry('щ', "sht"), Map.entry('ъ', "a"),
            Map.entry('ь', ""), Map.entry('ю', "yu"), Map.entry('я', "ya")
    );

    private ProductSlugGenerator() {
    }

    public static String toSlug(String value) {
        if (value == null || value.isBlank()) {
            return "product";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .toLowerCase(Locale.ROOT);
        StringBuilder latin = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(codePoint -> {
            if (Character.getType(codePoint) == Character.NON_SPACING_MARK) {
                return;
            }
            char character = (char) codePoint;
            latin.append(CYRILLIC.getOrDefault(character, String.valueOf(character)));
        });

        String slug = latin.toString()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            return "product";
        }
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }
}
