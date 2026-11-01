package nevg.autosilent.Service;

import nevg.autosilent.Models.Dto.*;
import nevg.autosilent.Models.Entity.*;
import nevg.autosilent.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PromotionService {
    private final PromotionRepository promotions;
    private final ProductRepository products;
    private final ProductService catalog;
    private final MessageSource messages;
    public PromotionService(PromotionRepository promotions, ProductRepository products, ProductService catalog, MessageSource messages) {
        this.promotions = promotions; this.products = products; this.catalog = catalog; this.messages = messages;
    }

    public record Card(Long id, Long promotionId, String url, String name, String model, String sku,
            String category, BigDecimal price, BigDecimal originalPrice, String description, int stock,
            String mainImageUrl, int units, String promotionUrl) {
        public String displayName() { return name + " · " + units + " " + (LocaleContextHolder.getLocale().getLanguage().equals("bg") ? "бр." : "pcs"); }
    }
    @Transactional(readOnly = true)
    public List<Promotion> all() { return promotions.findAllByOrderByIdDesc(); }

    @Transactional
    public void save(Long id, PromotionForm form) {
        if (form.endsAt() != null && form.startsAt() != null && !form.endsAt().isAfter(form.startsAt())) throw invalid("promotion.invalidDates");
        if (form.productId() == null || form.units() < 1 || form.price() == null || form.price().signum() <= 0
                || form.price().scale() > 2 || form.price().compareTo(new BigDecimal("99999999.99")) > 0) throw invalid("promotion.invalid");
        Product product = products.findById(form.productId()).filter(p -> !Boolean.TRUE.equals(form.active()) || p.isActive()).orElseThrow(() -> invalid("order.error.productUnavailable"));
        var offer = id == null ? new Promotion() : promotions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        offer.setProduct(product); offer.setUnits(form.units()); offer.setPrice(form.price()); offer.setActive(Boolean.TRUE.equals(form.active()));
        offer.setHideFromCatalog(Boolean.TRUE.equals(form.hideFromCatalog())); offer.setStartsAt(form.startsAt()); offer.setEndsAt(form.endsAt());
        assignSlug(offer);
        promotions.save(offer);
    }

    @Transactional
    public void initializeSlugs() {
        for (var offer : promotions.findAllByOrderByIdAsc()) {
            if (offer.getSlug() == null || offer.getSlug().isBlank()) {
                assignSlug(offer);
                promotions.saveAndFlush(offer);
            }
        }
    }

    private void assignSlug(Promotion offer) {
        if (offer.getSlug() != null && !offer.getSlug().isBlank()) return;
        String base = offer.getProduct().getUrl();
        if (base.matches("\\d+")) base = "product-" + base;
        String slug = base;
        if (promotions.existsBySlug(slug)) {
            String pack = base + "-" + offer.getUnits() + "-br";
            slug = pack;
            for (int number = 2; promotions.existsBySlug(slug); number++) slug = pack + "-" + number;
        }
        offer.setSlug(slug);
    }

    @Transactional(readOnly = true)
    public Card getBySlug(String slug) {
        return card(promotions.findBySlug(slug).filter(p -> p.availableAt(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<Card> active() {
        LocalDateTime now = LocalDateTime.now();
        return promotions.findAllByOrderByIdDesc().stream().filter(p -> p.availableAt(now)).map(this::card).toList();
    }

    @Transactional(readOnly = true)
    public Card get(Long id) {
        var offer = promotions.findById(id).filter(p -> p.availableAt(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return card(offer);
    }

    @Transactional(readOnly = true)
    public ProductDetailsDto details(Card offer) {
        var product = catalog.getActiveProduct(offer.id()).orElseThrow();
        return new ProductDetailsDto(product.id(), product.url(), offer.displayName(), null, product.sku(),
                product.category(), offer.price(), product.description(), offer.stock(), product.count(),
                product.imageUrls(), product.secondaryCategory());
    }

    @Transactional(readOnly = true)
    public List<ProductViewDto> related(Long productId) {
        var now = LocalDateTime.now();
        Set<Long> promoted = promotions.findAllByOrderByIdDesc().stream().filter(p -> p.availableAt(now))
                .map(p -> p.getProduct().getId()).collect(java.util.stream.Collectors.toSet());
        return catalog.getActiveProducts().stream().filter(p -> !p.id().equals(productId) && !promoted.contains(p.id()))
                .limit(8).toList();
    }
    private Card card(Promotion offer) {
        var p = catalog.getActiveProduct(offer.getProduct().getId()).orElseThrow();
        return new Card(p.id(), offer.getId(), p.url(), p.displayName(), p.model(), p.sku(), p.category(), offer.getPrice(),
                p.price().multiply(BigDecimal.valueOf(offer.getUnits())), p.plainDescription(), p.stock() / offer.getUnits(),
                p.mainImageUrl(), offer.getUnits(), "/promocii/" + offer.getSlug());
    }
    private IllegalArgumentException invalid(String key) { return new IllegalArgumentException(messages.getMessage(key, null, LocaleContextHolder.getLocale())); }
}
