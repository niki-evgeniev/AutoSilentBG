package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            SELECT user FROM User user
            WHERE LOWER(user.email) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(user.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(user.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(CONCAT(CONCAT(user.firstName, ' '), user.lastName))
                    LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<User> searchByEmailOrName(@Param("query") String query, Pageable pageable);
}
