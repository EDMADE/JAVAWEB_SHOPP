package com.example.demo.model.dto;

import java.math.BigDecimal;

public class OrderItemDTO {
    private Long productId;
    private Long skuId;
    private Integer quantity;
    private BigDecimal price;
    private String productName;
    
    public OrderItemDTO() {}
    
    public OrderItemDTO(Long productId, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
    
    public OrderItemDTO(Long productId, Long skuId, Integer quantity, BigDecimal price, String productName) {
        this.productId = productId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.price = price;
        this.productName = productName;
    }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public BigDecimal getTotalPrice() {
        if (price != null && quantity != null) {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
    
    public boolean isValid() {
        return productId != null && 
               quantity != null && quantity > 0 && 
               price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }
    
    @Override
    public String toString() {
        return "OrderItemDTO{" +
                "productId=" + productId +
                ", skuId=" + skuId +
                ", quantity=" + quantity +
                ", price=" + price +
                ", productName='" + productName + '\'' +
                '}';
    }
}
