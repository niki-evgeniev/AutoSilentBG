package nevg.nirton.Models.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seo_product")
@NoArgsConstructor
@Getter
@Setter
public class Seo extends BaseEntity {

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "title" , columnDefinition = "TEXT")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "imageUrl" , columnDefinition = "TEXT")
    private String imageUrl;

    @ManyToOne
    private Product product;

}
