package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.IpAddress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IpAddressRepository extends JpaRepository<IpAddress, Long> {

    Optional<IpAddress> findByAddress(String address);

    @Modifying
    @Query(value = """
            INSERT INTO ip_addresses
                (uuid, ip_address, first_seen, last_seen, count_visits, is_banned, banned_until, user_id)
            VALUES
                (:uuid, :address, :now, :now, 0, false, null, null)
            ON DUPLICATE KEY UPDATE ip_address = :address
            """, nativeQuery = true)
    int insertIfAbsent(@Param("uuid") UUID uuid,
                       @Param("address") String address,
                       @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ip from IpAddress ip where ip.address = :address")
    Optional<IpAddress> findByAddressForUpdate(@Param("address") String address);
}
