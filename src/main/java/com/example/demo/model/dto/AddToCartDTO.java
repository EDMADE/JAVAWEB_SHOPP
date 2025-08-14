package com.example.demo.model.dto;

import lombok.Data;

@Data
public class AddToCartDTO {
    private Long productId;
    private Long skuId;
    private Integer quantity;
}
