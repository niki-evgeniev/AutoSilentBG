package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import nevg.autosilent.Models.Enums.RoleType;

import java.math.BigDecimal;

public class AdminUserEditDto {
    @NotBlank
    @Size(min = 2, max = 50)
    private String firstName;
    @NotBlank
    @Size(min = 2, max = 50)
    private String lastName;
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;
    @Pattern(regexp = "^$|^(?=(?:.*\\d){7,})[+]?[0-9 ()-]{7,20}$")
    private String phoneNumber;
    @NotNull
    private RoleType role;
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private boolean blocked;
    @Pattern(regexp = "^$|.{8,72}$")
    private String newPassword;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
