package nevg.nirton.Models.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "products")
@NoArgsConstructor
@Getter
@Setter
public class Product extends BaseEntity {


    @Column(name = "name_product", nullable = false, unique = true)
    private String nameProduct;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "url_link")
    private String url;

    @Column(name = "stock")
    private int stock;

    @Column(name = "sold")
    private int sold;

    @CreationTimestamp
    @Column(name = "add_date")
    private LocalDateTime addDate;

    @ManyToOne
    private User user;


}
