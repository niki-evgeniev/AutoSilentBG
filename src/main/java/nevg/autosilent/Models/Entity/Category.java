package nevg.autosilent.Models.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
@NoArgsConstructor
@Getter
@Setter
public class Category extends BaseEntity {

    @Column(name = "category", nullable = false, unique = true, length = 80)
    private String category;

    @OneToMany(mappedBy = "category")
    private List<Product> products = new ArrayList<>();
}
