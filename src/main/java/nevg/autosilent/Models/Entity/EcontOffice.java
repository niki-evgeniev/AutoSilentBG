package nevg.autosilent.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "econt_offices", indexes = @Index(columnList = "cityId"))
@Getter @Setter
public class EcontOffice {
    @Id private Long id;
    @Column(nullable = false) private String code;
    @Column(nullable = false) private String name;
    private Long cityId;
    @Column(length = 1000) private String address;
    private boolean active;
}
