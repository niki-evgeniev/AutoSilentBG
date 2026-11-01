package nevg.autosilent.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "promotions") @Getter @Setter
public class Promotion extends BaseEntity {
    @Column(unique = true, length = 240)
    private String slug;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false) private int units;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private boolean hideFromCatalog;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    public boolean availableAt(LocalDateTime now) {
        return active && product.isActive() && (startsAt == null || !now.isBefore(startsAt))
                && (endsAt == null || now.isBefore(endsAt));
    }
}
