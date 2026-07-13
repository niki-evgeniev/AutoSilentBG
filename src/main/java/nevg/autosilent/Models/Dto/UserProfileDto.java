package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserProfileDto {

    @NotBlank(message = "{validation.firstName.required}")
    @Size(min = 2, max = 50, message = "{validation.firstName.size}")
    @Pattern(regexp = "^\\p{L}(?:[\\p{L} '-]*\\p{L})?$", message = "{validation.firstName.pattern}")
    private String firstName;

    @NotBlank(message = "{validation.lastName.required}")
    @Size(min = 2, max = 50, message = "{validation.lastName.size}")
    @Pattern(regexp = "^\\p{L}(?:[\\p{L} '-]*\\p{L})?$", message = "{validation.lastName.pattern}")
    private String lastName;

    @Pattern(regexp = "^$|^(?=(?:.*\\d){7,})[+]?[0-9 ()-]{7,20}$",
            message = "{validation.phone.invalid}")
    private String phoneNumber;

    private String email;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
