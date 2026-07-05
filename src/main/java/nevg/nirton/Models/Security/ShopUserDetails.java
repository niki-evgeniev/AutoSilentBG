package nevg.nirton.Models.Security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public final class ShopUserDetails extends org.springframework.security.core.userdetails.User {

    private String firstName;

    public ShopUserDetails(String email,
                           String password,
                           String firstName,
                           Collection<? extends GrantedAuthority> authorities) {
        super(email, password, authorities);
        this.firstName = firstName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}
