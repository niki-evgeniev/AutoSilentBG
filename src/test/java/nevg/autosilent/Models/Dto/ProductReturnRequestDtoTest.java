package nevg.autosilent.Models.Dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import nevg.autosilent.Models.Enums.ReturnResolution;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProductReturnRequestDtoTest {

    @Test
    void completeReturnRequestIsValid() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(validRequest())).isEmpty();
        }
    }

    @Test
    void requiredFieldsAreValidated() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(new ProductReturnRequestDto()))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("firstName", "lastName", "email", "phone", "orderNumber",
                            "orderedOn", "productName", "productCode", "quantity", "resolution", "reason");
        }
    }

    @Test
    void futureOrderDateAndInvalidQuantityAreRejected() {
        ProductReturnRequestDto request = validRequest();
        request.setOrderedOn(LocalDate.now().plusDays(1));
        request.setQuantity(0);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("orderedOn", "quantity");
        }
    }

    private ProductReturnRequestDto validRequest() {
        ProductReturnRequestDto request = new ProductReturnRequestDto();
        request.setFirstName("Ivan");
        request.setLastName("Ivanov");
        request.setEmail("ivan@example.com");
        request.setPhone("+359 89 123 4567");
        request.setOrderNumber("NRT-20260101123000-AB12CD34");
        request.setOrderedOn(LocalDate.now());
        request.setProductName("Sound insulation model X");
        request.setProductCode("SKU-100");
        request.setQuantity(2);
        request.setResolution(ReturnResolution.REFUND);
        request.setReason("The product does not fit my vehicle.");
        return request;
    }
}
