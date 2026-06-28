package nevg.nirton.Models.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductCreateDto {

    @NotBlank(message = "Името на продукта е задължително.")
    @Size(min = 2, max = 150, message = "Името трябва да е между 2 и 150 символа.")
    private String nameProduct;

    @NotBlank(message = "Продуктовият код е задължителен.")
    @Size(max = 50, message = "Продуктовият код може да е до 50 символа.")
    private String sku;

    @NotBlank(message = "Категорията е задължителна.")
    @Size(max = 80, message = "Категорията може да е до 80 символа.")
    private String category;

    @NotNull(message = "Цената е задължителна.")
    @DecimalMin(value = "0.01", message = "Цената трябва да е по-голяма от 0.")
    @Digits(integer = 10, fraction = 2, message = "Цената може да има най-много 2 знака след десетичната запетая.")
    private BigDecimal price;

    @NotBlank(message = "Информацията за продукта е задължителна.")
    @Size(min = 10, max = 5000, message = "Информацията трябва да е между 10 и 5000 символа.")
    private String description;

    @Min(value = 0, message = "Наличността не може да бъде отрицателна.")
    private int stock;

    private boolean active = true;
    private MultipartFile mainImage;
    private List<MultipartFile> additionalImages = new ArrayList<>();

    public String getNameProduct() {
        return nameProduct;
    }

    public void setNameProduct(String nameProduct) {
        this.nameProduct = nameProduct;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public MultipartFile getMainImage() {
        return mainImage;
    }

    public void setMainImage(MultipartFile mainImage) {
        this.mainImage = mainImage;
    }

    public List<MultipartFile> getAdditionalImages() {
        return additionalImages;
    }

    public void setAdditionalImages(List<MultipartFile> additionalImages) {
        this.additionalImages = additionalImages == null ? new ArrayList<>() : additionalImages;
    }
}
