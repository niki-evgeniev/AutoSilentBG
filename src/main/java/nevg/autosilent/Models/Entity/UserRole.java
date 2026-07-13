package nevg.autosilent.Models.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.NoArgsConstructor;
import nevg.autosilent.Models.Enums.RoleType;

@Entity
@Table(name = "roles", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "role_type"))
@NoArgsConstructor
public class UserRole extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType roleType;


    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }
}
