package com.example.demo.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BidDTO {
    
    private Long bidId;
    private Long productId;
    private Long bidderId;
    private String bidderName;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private Boolean isNewHighest;
    private String message;
    public BidDTO() {}
    

    public BidDTO(Long bidId, Long productId, Long bidderId, String bidderName, 
                  BigDecimal amount, LocalDateTime bidTime, Boolean isNewHighest) {
        this.bidId = bidId;
        this.productId = productId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.bidTime = bidTime;
        this.isNewHighest = isNewHighest;
    }
    
    public Long getBidId() { return bidId; }
    public void setBidId(Long bidId) { this.bidId = bidId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Long getBidderId() { return bidderId; }
    public void setBidderId(Long bidderId) { this.bidderId = bidderId; }
    
    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }
    
    public Boolean getIsNewHighest() { return isNewHighest; }
    public void setIsNewHighest(Boolean isNewHighest) { this.isNewHighest = isNewHighest; }
}
