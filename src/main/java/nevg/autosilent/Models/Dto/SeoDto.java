package nevg.autosilent.Models.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SeoDto {

    @NotBlank(message = "{validation.seo.title.required}")
    @Size(max = 70, message = "{validation.seo.title.size}")
    private String title;

    @NotBlank(message = "{validation.seo.description.required}")
    @Size(max = 160, message = "{validation.seo.description.size}")
    private String description;

    @Size(max = 500, message = "{validation.seo.keywords.size}")
    private String keywords;

    @Size(max = 2048, message = "{validation.seo.imageUrl.size}")
    private String imageUrl;

    private String productName;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
