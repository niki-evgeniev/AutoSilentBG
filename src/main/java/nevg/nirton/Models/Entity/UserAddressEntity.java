package nevg.nirton.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nevg.nirton.Models.Enums.DeliveryType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_addresses")
@NoArgsConstructor
@Getter
@Setter
public class UserAddressEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    @Column(name = "country", nullable = false, length = 100)
    private String country = "България";

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "postcode", length = 20)
    private String postcode;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "courier_type", length = 50)
    private DeliveryType courierType;

    @Column(name = "courier_office_name", length = 255)
    private String courierOfficeName;

    @Column(name = "courier_office_code", length = 100)
    private String courierOfficeCode;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
