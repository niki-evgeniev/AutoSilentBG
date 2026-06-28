package nevg.nirton.Models.Dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRegistrationDto {

    @NotBlank(message = "Името е задължително.")
    @Size(min = 2, max = 50, message = "Името трябва да е между 2 и 50 символа.")
    @Pattern(regexp = "^\\p{L}(?:[\\p{L} '-]*\\p{L})?$", message = "Името съдържа невалидни символи.")
    private String firstName;

    @NotBlank(message = "Фамилията е задължителна.")
    @Size(min = 2, max = 50, message = "Фамилията трябва да е между 2 и 50 символа.")
    @Pattern(regexp = "^\\p{L}(?:[\\p{L} '-]*\\p{L})?$", message = "Фамилията съдържа невалидни символи.")
    private String lastName;

    @NotBlank(message = "Имейлът е задължителен.")
    @Email(message = "Въведете валиден имейл адрес.")
    @Size(max = 254, message = "Имейлът е прекалено дълъг.")
    private String email;

    @Pattern(regexp = "^$|^(?=(?:.*\\d){7,})[+]?[0-9 ()-]{7,20}$", message = "Въведете валиден телефонен номер.")
    private String phoneNumber;

    @NotBlank(message = "Паролата е задължителна.")
    @Size(min = 8, max = 72, message = "Паролата трябва да е между 8 и 72 символа.")
    private String password;

    @NotBlank(message = "Повторете паролата.")
    private String confirmPassword;

    @AssertTrue(message = "Трябва да приемете общите условия.")
    private boolean termsAccepted;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }
}
