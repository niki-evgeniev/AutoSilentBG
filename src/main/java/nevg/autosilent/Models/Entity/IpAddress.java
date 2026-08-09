package nevg.autosilent.Models.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ip_addresses")
@Getter
@Setter
@NoArgsConstructor
public class IpAddress extends BaseEntity {

    @Column(name = "ip_address", nullable = false, unique = true, length = 45)
    private String address;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "count_visits", nullable = false)
    private Long countVisits = 0L;

    @Column(name = "visits_date")
    private LocalDate visitsDate;

    @Column(name = "visits_today", nullable = false, columnDefinition = "BIGINT NOT NULL DEFAULT 0")
    private long visitsToday;

    @Column(name = "is_banned", nullable = false)
    private boolean banned;

    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
