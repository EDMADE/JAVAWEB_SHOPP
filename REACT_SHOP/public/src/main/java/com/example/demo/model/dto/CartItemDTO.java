package com.example.demo.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long cartItemId;
    private Long productId;
    private Long skuId;           
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal price;     
    private String skuSpec; 
    private String imageUrl;
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    
    public BigDecimal getSubtotal() {
        if (price != null && quantity != null) {
            return price.multiply(new BigDecimal(quantity));
        }
        return BigDecimal.ZERO;
    }
    
   
    public String getFormattedPrice() {
        if (price != null) {
            return "NT$ " + price.toString();
        }
        return "NT$ 0";
    }
    

    public String getFormattedSubtotal() {
        BigDecimal subtotal = getSubtotal();
        return "NT$ " + subtotal.toString();
    }
}
