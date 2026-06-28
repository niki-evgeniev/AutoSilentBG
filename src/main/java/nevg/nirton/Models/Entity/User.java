package nevg.nirton.Models.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class User extends BaseEntity {
    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "register_date", columnDefinition = "DATETIME(0)")
    private LocalDateTime registerDate;

    @Column(name = "edit_date", columnDefinition = "DATETIME(0)")
    private LocalDateTime editDate;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "is_activate")
//    @Access(AccessType.FIELD)
    private boolean activate = false;

    @Column(name = "token_created", columnDefinition = "DATETIME(0)")
    private LocalDateTime tokenCreated;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<UserRole> roles = new HashSet<>();
}


