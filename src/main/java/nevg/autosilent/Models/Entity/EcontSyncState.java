package nevg.autosilent.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "econt_sync_state")
@Getter @Setter
public class EcontSyncState {
    @Id private Long id = 1L;
    private LocalDateTime lastSuccess;
    @Column(length = 500) private String lastError;
}
