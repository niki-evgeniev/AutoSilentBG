package nevg.autosilent.Utility;

import nevg.autosilent.Models.Entity.OrderItemEntity;
import nevg.autosilent.Models.Entity.Picture;
import nevg.autosilent.Models.Entity.Product;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

public final class OrderItemImage {
    private OrderItemImage() {}

    public static String resolve(OrderItemEntity item) {
        String snapshot = item.getProductImageUrl();
        return snapshot != null && !snapshot.isBlank() ? snapshot : mainImageUrl(item.getProduct());
    }

    public static String mainImageUrl(Product product) {
        if (product == null) return null;
        return product.getPictures().stream().filter(Picture::isMainImage).findFirst()
                .map(picture -> "/ProductImages/"
                        + UriUtils.encodePathSegment(directoryName(product.getDisplayName()), StandardCharsets.UTF_8)
                        + "/" + UriUtils.encodePathSegment(picture.getFileName(), StandardCharsets.UTF_8))
                .orElse(null);
    }

    private static String directoryName(String name) {
        return Normalizer.normalize(name.trim(), Normalizer.Form.NFC)
                .replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "-")
                .replaceAll("\\s+", "-").replaceAll("-+", "-").replaceAll("^[. -]+|[. -]+$", "");
    }
}
