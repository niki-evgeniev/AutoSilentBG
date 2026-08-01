package nevg.autosilent.Models.Dto;

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

    @NotBlank(message = "{validation.product.name.required}")
    @Size(min = 2, max = 150, message = "{validation.product.name.size}")
    private String nameProduct;

    @NotBlank(message = "{validation.product.model.required}")
    @Size(min = 1, max = 150, message = "{validation.product.model.size}")
    private String model;

    @NotNull(message = "{validation.product.category.required}")
    private Long categoryId;

    @NotNull(message = "{validation.product.price.required}")
    @DecimalMin(value = "0.01", message = "{validation.product.price.min}")
    @Digits(integer = 10, fraction = 2, message = "{validation.product.price.digits}")
    private BigDecimal price;

    @NotBlank(message = "{validation.product.description.required}")
    @Size(min = 10, max = 10000, message = "{validation.product.description.size}")
    private String description;

    @Min(value = 0, message = "{validation.product.stock.min}")
    private int stock;

    private boolean active = true;
    private MultipartFile mainImage;
    private List<MultipartFile> additionalImages = new ArrayList<>();
    private List<Long> removedImageIds = new ArrayList<>();
    private Long existingMainImageId;
    private List<ProductImageEditDto> existingImages = new ArrayList<>();

    public String getNameProduct() {
        return nameProduct;
    }

    public void setNameProduct(String nameProduct) {
        this.nameProduct = nameProduct;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public List<Long> getRemovedImageIds() {
        return removedImageIds;
    }

    public void setRemovedImageIds(List<Long> removedImageIds) {
        this.removedImageIds = removedImageIds == null ? new ArrayList<>() : removedImageIds;
    }

    public Long getExistingMainImageId() {
        return existingMainImageId;
    }

    public void setExistingMainImageId(Long existingMainImageId) {
        this.existingMainImageId = existingMainImageId;
    }

    public List<ProductImageEditDto> getExistingImages() {
        return existingImages;
    }

    public void setExistingImages(List<ProductImageEditDto> existingImages) {
        this.existingImages = existingImages == null ? new ArrayList<>() : existingImages;
    }
}
