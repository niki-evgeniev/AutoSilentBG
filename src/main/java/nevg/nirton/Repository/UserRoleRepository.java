package nevg.nirton.Repository;

import nevg.nirton.Models.Entity.UserRole;
import nevg.nirton.Models.Enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findByRoleType(RoleType roleType);
}
