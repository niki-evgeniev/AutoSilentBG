package nevg.autosilent.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "products")
@NoArgsConstructor
@Getter
@Setter
public class Product extends BaseEntity {


    @Column(name = "name_product", nullable = false, unique = true, length = 150)
    private String nameProduct;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "category", nullable = false, length = 80)
    private String category;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "url_link")
    private String url;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("mainImage DESC, id ASC")
    private List<Picture> pictures = new ArrayList<>();

    @Column(name = "stock")
    private int stock;

    @Column(name = "sold")
    private int sold;

    @Column(name = "view_count", nullable = false)
    private long count;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "add_date")
    private LocalDateTime addDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void addPicture(Picture picture) {
        picture.setProduct(this);
        pictures.add(picture);
    }

    public void removePicture(Picture picture) {
        pictures.remove(picture);
        picture.setProduct(null);
    }

    public void changeMainPicture(Picture newMainPicture) {
        if (!pictures.contains(newMainPicture)) {
            throw new IllegalArgumentException("Снимката не принадлежи на този продукт.");
        }
        pictures.forEach(picture -> picture.setMainImage(picture == newMainPicture));
    }

    public void incrementCount() {
        count++;
    }

}
