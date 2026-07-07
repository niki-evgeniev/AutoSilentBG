package nevg.nirton.Repository;

import nevg.nirton.Models.Entity.IpAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpAddressRepository extends JpaRepository<IpAddress, Long> {

    Optional<IpAddress> findByAddress(String address);
}
