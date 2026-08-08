package nevg.autosilent.Models.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "favorites", uniqueConstraints =
@UniqueConstraint(name = "uk_favorites_user_product", columnNames = {"user_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Favorite extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
