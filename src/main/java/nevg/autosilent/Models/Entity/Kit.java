package nevg.autosilent.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kits")
@Getter
@Setter
@NoArgsConstructor
public class Kit extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_product_id", nullable = false, unique = true)
    private Product catalogProduct;

    @OneToMany(mappedBy = "kit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<KitItem> items = new ArrayList<>();

    public void addItem(Product product, int quantity) {
        KitItem item = new KitItem();
        item.setKit(this);
        item.setProduct(product);
        item.setQuantity(quantity);
        items.add(item);
    }

    public int availableStock() {
        return items.stream()
                .mapToInt(item -> item.getProduct().isActive()
                        ? item.getProduct().getStock() / item.getQuantity() : 0)
                .min().orElse(0);
    }
}
