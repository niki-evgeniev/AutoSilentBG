package nevg.autosilent.Models.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalog_seo")
@NoArgsConstructor
@Getter
@Setter
public class CatalogSeo extends BaseEntity {

    @Column(name = "title", nullable = false, length = 70)
    private String title;

    @Column(name = "description", nullable = false, length = 160)
    private String description;

    @Column(name = "keywords", length = 500)
    private String keywords;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;
}
