package nevg.autosilent.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;


@Entity
@Table(name = "products", uniqueConstraints =
@UniqueConstraint(name = "uk_products_brand_model", columnNames = {"name_product", "model"}))
@NoArgsConstructor
@Getter
@Setter
public class Product extends BaseEntity {

    @Column(name = "name_product", nullable = false, length = 150)
    private String nameProduct;

    @Column(name = "model", length = 150)
    private String model;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secondary_category_id")
    private Category secondaryCategory;

    @ManyToMany
    @JoinTable(name = "product_additional_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_product_additional_category",
                    columnNames = {"product_id", "category_id"}))
    @OrderBy("category ASC")
    private Set<Category> additionalCategories = new LinkedHashSet<>();

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "url_link", nullable = false, unique = true, length = 180)
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

    @Column(name = "content_updated_at")
    private LocalDateTime contentUpdatedAt;

    @PrePersist
    private void initializeContentDate() {
        if (contentUpdatedAt == null) contentUpdatedAt = LocalDateTime.now();
    }

    private void contentChanged(Object previous, Object next) {
        if (!java.util.Objects.equals(previous, next)) contentUpdatedAt = LocalDateTime.now();
    }

    public void setNameProduct(String value) { contentChanged(nameProduct, value); nameProduct = value; }
    public void setModel(String value) { contentChanged(model, value); model = value; }
    public void setSku(String value) { contentChanged(sku, value); sku = value; }
    public void setDescription(String value) { contentChanged(description, value); description = value; }
    public void setUrl(String value) { contentChanged(url, value); url = value; }
    public void setPrice(BigDecimal value) { contentChanged(price, value); price = value; }
    public void setStock(int value) { contentChanged(stock, value); stock = value; }
    public void setActive(boolean value) { contentChanged(active, value); active = value; }
    public void setCategory(Category value) { contentChanged(category, value); category = value; }
    public void setSecondaryCategory(Category value) { contentChanged(secondaryCategory, value); secondaryCategory = value; }

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void addPicture(Picture picture) {
        contentUpdatedAt = LocalDateTime.now();
        picture.setProduct(this);
        pictures.add(picture);
    }

    public void removePicture(Picture picture) {
        contentUpdatedAt = LocalDateTime.now();
        pictures.remove(picture);
        picture.setProduct(null);
    }

    public void changeMainPicture(Picture newMainPicture) {
        if (!pictures.contains(newMainPicture)) {
            throw new IllegalArgumentException("Снимката не принадлежи на този продукт.");
        }
        pictures.forEach(picture -> picture.setMainImage(picture == newMainPicture));
        contentUpdatedAt = LocalDateTime.now();
    }

    public void incrementCount() {
        count++;
    }

    public String getDisplayName() {
        return model == null || model.isBlank() ? nameProduct : nameProduct + " " + model;
    }

}
