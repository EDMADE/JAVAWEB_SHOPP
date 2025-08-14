package com.example.demo.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class ProductCreateDTO {
    @NotBlank(message = "商品名稱不能為空")
    private String name;

    private Long seller_id;
    
    @NotBlank(message = "分類不能為空")
    private String category;
    
    @NotBlank(message = "商品狀況不能為空")
    private String product_condition;
    
    private String description;
    
    @Pattern(regexp = "^$|^[1-9]\\d*(\\.\\d+)?$", message = "起標價格式錯誤或必須大於0")
    private String start_price;


    @Pattern(regexp = "^$|^[1-9]\\d*(\\.\\d+)?$", message = "目前價格格式錯誤或必須大於0")
    private String current_price;

  
    @Pattern(regexp = "^$|^\\d+$", message = "庫存數量必須是非負整數")
    private String stock_quantity;

    private String bid_end_time;
    private String specifications;
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
