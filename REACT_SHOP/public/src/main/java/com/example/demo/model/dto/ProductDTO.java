package com.example.demo.model.dto;

import com.example.demo.model.entity.ProductSku;
import com.example.demo.model.entity.ProductImage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ProductDTO {
    private Long id;
    private Long sellerId;
    private String name;
    private String category;
    private String description;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private Integer stockQuantity;
    private String mainImageUrl;
    private LocalDateTime bidEndTime;
    private String status;
    private LocalDateTime createdAt;
    private String specifications;
    private String productCondition;
    
    // 聚合欄位
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer totalStock;
    private List<ProductSku> skus;
    
    //多圖支援
    private List<ProductImage> productImages;

    
    public ProductDTO() {}

  
    public ProductDTO(Long id, Long sellerId, String name, String category, String description,
                     BigDecimal startPrice, BigDecimal currentPrice, Integer stockQuantity,
                     String mainImageUrl, LocalDateTime bidEndTime, String status,
                     LocalDateTime createdAt, String specifications, String productCondition) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.stockQuantity = stockQuantity;
        this.mainImageUrl = mainImageUrl;
        this.bidEndTime = bidEndTime;
        this.status = status;
        this.createdAt = createdAt;
        this.specifications = specifications;
        this.productCondition = productCondition;
    }

 
    public ProductDTO(Long id, Long sellerId, String name, String category, String description,
                     BigDecimal startPrice, BigDecimal currentPrice, Integer stockQuantity,
                     String mainImageUrl, LocalDateTime bidEndTime, String status,
                     LocalDateTime createdAt, String specifications, String productCondition,
                     BigDecimal minPrice, BigDecimal maxPrice, Long totalStock) {
        this(id, sellerId, name, category, description, startPrice, currentPrice, stockQuantity,
             mainImageUrl, bidEndTime, status, createdAt, specifications, productCondition);
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.totalStock = totalStock != null ? totalStock.intValue() : null;
    }

   
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getStartPrice() { return startPrice; }
    public void setStartPrice(BigDecimal startPrice) { this.startPrice = startPrice; }
    
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    

    public String getMainImageUrl() { 
        if (productImages != null && !productImages.isEmpty()) {
            return productImages.get(0).getImageUrl();
        }
        return mainImageUrl; 
    }
    
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }
    
    public LocalDateTime getBidEndTime() { return bidEndTime; }
    public void setBidEndTime(LocalDateTime bidEndTime) { this.bidEndTime = bidEndTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getSpecifications() { return specifications; }
    public void setSpecifications(String specifications) { this.specifications = specifications; }
    
    public String getProductCondition() { return productCondition; }
    public void setProductCondition(String productCondition) { this.productCondition = productCondition; }
    
 
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    
    public Integer getTotalStock() { return totalStock; }
    public void setTotalStock(Integer totalStock) { this.totalStock = totalStock; }
    
    public List<ProductSku> getSkus() { return skus; }
    public void setSkus(List<ProductSku> skus) { this.skus = skus; }

    
    public List<ProductImage> getProductImages() {
        return productImages;
    }

    public void setProductImages(List<ProductImage> productImages) {
        this.productImages = productImages;
    }


    public List<String> getAllImageUrls() {
        if (productImages != null && !productImages.isEmpty()) {
            return productImages.stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());
        }
        return mainImageUrl != null ? Arrays.asList(mainImageUrl) : new ArrayList<>();
    }

   
    public boolean hasMultipleImages() {
        return productImages != null && productImages.size() > 1;
    }

  
    public int getImageCount() {
        if (productImages != null) {
            return productImages.size();
        }
        return mainImageUrl != null ? 1 : 0;
    }

    
    public String getImageUrl(int index) {
        if (productImages != null && index >= 0 && index < productImages.size()) {
            return productImages.get(index).getImageUrl();
        }
        if (index == 0 && mainImageUrl != null) {
            return mainImageUrl;
        }
        return null;
    }

 
    public String getDisplayPrice() {
        if (minPrice != null && maxPrice != null && !minPrice.equals(maxPrice)) {
            return "NT$ " + minPrice + " - " + maxPrice;
        } else if (minPrice != null) {
            return "NT$ " + minPrice;
        } else if (currentPrice != null) {
            return "NT$ " + currentPrice;
        } else {
            return "NT$ 0";
        }
    }

    public Integer getDisplayStock() {
        if (totalStock != null) {
            return totalStock;
        } else if (stockQuantity != null) {
            return stockQuantity;
        } else {
            return 0;
        }
    }

    public boolean hasMultipleSkus() {
        return skus != null && skus.size() > 1;
    }

   
    @Override
    public String toString() {
        return "ProductDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", currentPrice=" + currentPrice +
                ", stockQuantity=" + stockQuantity +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", totalStock=" + totalStock +
                ", imageCount=" + getImageCount() +
                ", hasMultipleImages=" + hasMultipleImages() +
                '}';
    }
}
