package nevg.nirton.Models.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_inquiries")
@Getter
@Setter
@NoArgsConstructor
public class ContactInquiry extends BaseEntity {

    @Column(name = "sender_name", nullable = false, length = 100)
    private String name;

    @Column(name = "sender_email", nullable = false, length = 254)
    private String email;

    @Column(name = "subject", nullable = false, length = 150)
    private String subject;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;
}
