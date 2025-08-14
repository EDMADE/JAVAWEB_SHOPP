package com.example.demo.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 200)
    private String name;

    private String category;
    private String description;

    @Column(name = "start_price", precision = 38, scale = 2)
    private BigDecimal startPrice;

    @Column(name = "current_price", precision = 38, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;
    
    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "main_image_url")
    private String mainImageUrl;
    
    @Column(name = "bid_end_time")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime bidEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "product_condition")
    private String productCondition;

    
    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<ProductSku> skus;


    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<ProductImage> productImages;

  
    public enum ProductStatus {
        ACTIVE("上架中"),
        INACTIVE("已下架"),
        AUCTION("競標中"),
        AUCTION_ENDED("競標結束"),
        SOLD("已售出"),
        EXPIRED("已過期");
        
        private final String displayName;
        
        ProductStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isAuctionRelated() {
            return this == AUCTION || this == AUCTION_ENDED;
        }
        
        public boolean isActive() {
            return this == ACTIVE || this == AUCTION;
        }
        
        public static ProductStatus fromString(String status) {
            if (status == null || status.trim().isEmpty()) {
                return ACTIVE;
            }
            
            try {
                return ProductStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown ProductStatus: " + status + ", using ACTIVE as default");
                return ACTIVE;
            }
        }
    }
    
 
    public boolean isAuctionProduct() {
        return this.status != null && this.status.isAuctionRelated();
    }
    
    public boolean isAuctionActive() {
        return this.status == ProductStatus.AUCTION && 
               this.bidEndTime != null && 
               LocalDateTime.now().isBefore(this.bidEndTime);
    }
    
    public boolean isAuctionEnded() {
        return this.status == ProductStatus.AUCTION_ENDED || 
               (this.status == ProductStatus.AUCTION && 
                this.bidEndTime != null && 
                LocalDateTime.now().isAfter(this.bidEndTime));
    }
    
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }
    

    public String getMainImageUrl() {
      
    	 System.out.println("🖼️ 原始圖片路徑: " + this.mainImageUrl);
    	    
    	    if (this.mainImageUrl == null || 
    	        this.mainImageUrl.trim().isEmpty() || 
    	        this.mainImageUrl.equals("default.png") ||
    	        this.mainImageUrl.equals("/uploads/default.png")) {
    	        
    	        System.out.println("🖼️ 圖片為空，回傳 null");
    	        return null;
    	    }
    	    
    	    String result;
    	    if (!this.mainImageUrl.startsWith("/")) {
    	        result = "/uploads/" + this.mainImageUrl;
    	    } else {
    	        result = this.mainImageUrl;
    	    }
    	    
    	    System.out.println("🖼️ 處理後圖片路徑: " + result);
    	    return result;
    	}
    
  
    public BigDecimal getMinPrice() {
        if (skus != null && !skus.isEmpty()) {
            return skus.stream()
                .map(ProductSku::getPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(currentPrice != null ? currentPrice : startPrice);
        }
        return currentPrice != null ? currentPrice : startPrice;
    }
    

    public BigDecimal getMaxPrice() {
        if (skus != null && !skus.isEmpty()) {
            return skus.stream()
                .map(ProductSku::getPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .max(BigDecimal::compareTo)
                .orElse(currentPrice != null ? currentPrice : startPrice);
        }
        return currentPrice != null ? currentPrice : startPrice;
    }
    
  
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ProductStatus.ACTIVE;
        }
        if (this.currentPrice == null && this.startPrice != null) {
            this.currentPrice = this.startPrice;
        }
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", sellerId=" + sellerId +
                ", status=" + status +
                '}';
    }
}
