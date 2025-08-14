package com.example.demo.service;

import com.example.demo.model.dto.AddToCartDTO;
import com.example.demo.model.dto.CartResponseDTO;

public interface CartService {
    CartResponseDTO getCart(Long userId);
    CartResponseDTO addToCart(Long userId, AddToCartDTO dto);
    CartResponseDTO updateCartItem(Long userId, Long cartItemId, Integer quantity);
    void removeCartItem(Long userId, Long cartItemId);
    void clearUserCart(Long userId);
}
