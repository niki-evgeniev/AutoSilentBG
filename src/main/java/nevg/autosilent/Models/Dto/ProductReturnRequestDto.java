package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import nevg.autosilent.Models.Enums.ReturnResolution;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ProductReturnRequestDto {

    @NotBlank(message = "{validation.return.firstName.required}")
    @Size(max = 100, message = "{validation.return.firstName.size}")
    private String firstName;

    @NotBlank(message = "{validation.return.lastName.required}")
    @Size(max = 100, message = "{validation.return.lastName.size}")
    private String lastName;

    @NotBlank(message = "{validation.return.email.required}")
    @Email(message = "{validation.return.email.invalid}")
    @Size(max = 254, message = "{validation.return.email.size}")
    private String email;

    @NotBlank(message = "{validation.return.phone.required}")
    @Pattern(regexp = "^[0-9+()\\s-]{6,30}$", message = "{validation.return.phone.invalid}")
    private String phone;

    @NotBlank(message = "{validation.return.orderNumber.required}")
    @Size(max = 50, message = "{validation.return.orderNumber.size}")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "{validation.return.orderNumber.invalid}")
    private String orderNumber;

    @NotNull(message = "{validation.return.orderedOn.required}")
    @PastOrPresent(message = "{validation.return.orderedOn.pastOrPresent}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderedOn;

    @NotBlank(message = "{validation.return.productName.required}")
    @Size(max = 200, message = "{validation.return.productName.size}")
    private String productName;

    @NotBlank(message = "{validation.return.productCode.required}")
    @Size(max = 100, message = "{validation.return.productCode.size}")
    private String productCode;

    @NotNull(message = "{validation.return.quantity.required}")
    @Min(value = 1, message = "{validation.return.quantity.range}")
    @Max(value = 9999, message = "{validation.return.quantity.range}")
    private Integer quantity;

    @NotNull(message = "{validation.return.resolution.required}")
    private ReturnResolution resolution;

    @NotBlank(message = "{validation.return.reason.required}")
    @Size(min = 10, max = 2000, message = "{validation.return.reason.size}")
    private String reason;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDate getOrderedOn() {
        return orderedOn;
    }

    public void setOrderedOn(LocalDate orderedOn) {
        this.orderedOn = orderedOn;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public ReturnResolution getResolution() {
        return resolution;
    }

    public void setResolution(ReturnResolution resolution) {
        this.resolution = resolution;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
