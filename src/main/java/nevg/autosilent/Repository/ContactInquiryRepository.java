package nevg.autosilent.Repository;

import nevg.autosilent.Models.Entity.ContactInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Long> {

    Page<ContactInquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
