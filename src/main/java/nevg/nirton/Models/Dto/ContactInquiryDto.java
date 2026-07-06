package nevg.nirton.Models.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class ContactInquiryDto {

    @NotBlank(message = "{validation.contact.name.required}")
    @Size(min = 2, max = 100, message = "{validation.contact.name.size}")
    private String name;

    @NotBlank(message = "{validation.contact.email.required}")
    @Email(message = "{validation.contact.email.invalid}")
    @Size(max = 254, message = "{validation.contact.email.size}")
    private String email;

    @NotBlank(message = "{validation.contact.subject.required}")
    @Size(min = 3, max = 150, message = "{validation.contact.subject.size}")
    private String subject;

    @NotBlank(message = "{validation.contact.message.required}")
    @Size(min = 10, max = 5000, message = "{validation.contact.message.size}")
    private String message;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
