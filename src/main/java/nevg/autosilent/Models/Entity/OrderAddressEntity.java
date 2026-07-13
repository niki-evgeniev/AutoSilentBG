package nevg.autosilent.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_addresses")
@NoArgsConstructor
@Getter
@Setter
public class OrderAddressEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private OrderEntity order;

    @Column(name = "country", nullable = false, length = 100)
    private String country = "България";

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "postcode", length = 20)
    private String postcode;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "courier_office_name", length = 255)
    private String courierOfficeName;

    @Column(name = "courier_office_code", length = 100)
    private String courierOfficeCode;
}
