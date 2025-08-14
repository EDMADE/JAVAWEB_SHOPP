package com.example.demo.model.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderCreateDTO {
    private Long userId;
    private List<OrderItemDTO> orderItems;
    private BigDecimal totalAmount;
    private ShippingInfoDTO shippingInfo;
    
    public static class ShippingInfoDTO {
        private String name;
        private String phone;
        private String address;
        private String note;
        
        public ShippingInfoDTO() {}
        
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
    
    public OrderCreateDTO() {}
    

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public List<OrderItemDTO> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItemDTO> orderItems) { this.orderItems = orderItems; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public ShippingInfoDTO getShippingInfo() { return shippingInfo; }
    public void setShippingInfo(ShippingInfoDTO shippingInfo) { this.shippingInfo = shippingInfo; }
    
    public boolean isValid() {
        return userId != null && 
               orderItems != null && !orderItems.isEmpty() && 
               totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0 &&
               shippingInfo != null &&
               shippingInfo.getName() != null && !shippingInfo.getName().trim().isEmpty() &&
               shippingInfo.getPhone() != null && !shippingInfo.getPhone().trim().isEmpty() &&
               shippingInfo.getAddress() != null && !shippingInfo.getAddress().trim().isEmpty();
    }
}
