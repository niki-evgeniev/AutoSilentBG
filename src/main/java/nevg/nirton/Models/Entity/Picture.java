package nevg.nirton.Models.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pictures", uniqueConstraints =
        @UniqueConstraint(name = "uk_picture_product_filename", columnNames = {"product_id", "file_name"}))
@NoArgsConstructor
@Getter
@Setter
public class Picture extends BaseEntity {

    @Column(name = "file_name", nullable = false, length = 100)
    private String fileName;

    @Column(name = "is_main_image", nullable = false)
    private boolean mainImage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
