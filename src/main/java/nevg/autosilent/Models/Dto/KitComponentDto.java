package nevg.autosilent.Models.Dto;

public class KitComponentDto {
    private Long productId;
    private Integer quantity = 1;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
