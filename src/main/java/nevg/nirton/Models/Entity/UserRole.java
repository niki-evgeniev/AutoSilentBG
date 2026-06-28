package nevg.nirton.Models.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nevg.nirton.Models.Enums.RoleType;

@Entity
@Table(name = "users_roles")
@NoArgsConstructor
@Getter
@Setter
public class UserRole extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private RoleType roleType;
}
