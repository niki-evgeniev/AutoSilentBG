package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PromoCodeCreateDto {

    @NotBlank(message = "{validation.promoCode.code.required}")
    @Size(max = 40, message = "{validation.promoCode.code.size}")
    @Pattern(regexp = "^(?=(?:.*[A-Za-zА-Яа-я]){4,})[A-Za-zА-Яа-я0-9_-]{4,40}$",
            message = "{validation.promoCode.code.pattern}")
    private String code;

    @NotNull(message = "{validation.promoCode.discount.required}")
    private Integer discountPercent;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }
}
