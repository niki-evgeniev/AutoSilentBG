package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.UserRole;
import nevg.autosilent.Models.Enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    Optional<UserRole> findByRoleType(RoleType roleType);
}
