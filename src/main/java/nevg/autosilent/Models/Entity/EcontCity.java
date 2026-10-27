package nevg.autosilent.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "econt_cities")
@Getter @Setter
public class EcontCity {
    @Id private Long id;
    @Column(nullable = false) private String name;
    private String region;
    private String postcode;
    private boolean active;
}
