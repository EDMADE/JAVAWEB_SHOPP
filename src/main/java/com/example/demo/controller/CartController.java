package com.example.demo.controller;

import com.example.demo.model.dto.AddToCartDTO;
import com.example.demo.model.dto.CartResponseDTO;
import com.example.demo.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    
    @GetMapping
    public ResponseEntity<?> getCart(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        try {
            CartResponseDTO cart = cartService.getCart(userId);
            
            
            if (cart != null && cart.getItems() != null) {
                return ResponseEntity.ok(Map.of("items", cart.getItems()));
            } else {
                return ResponseEntity.ok(Map.of("items", java.util.Collections.emptyList()));
            }
            
        } catch (Exception e) {
            System.err.println("載入購物車失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "載入購物車失敗"));
        }
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        try {
            cartService.clearUserCart(userId);
            return ResponseEntity.ok(Map.of(
                "message", "購物車已清空",
                "success", true
            ));
        } catch (Exception e) {
            System.err.println("清空購物車失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "清空購物車失敗"));
        }
    }


   
    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody AddToCartDTO dto, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        try {
            CartResponseDTO cart = cartService.addToCart(userId, dto);
            return ResponseEntity.ok(Map.of(
                "items", cart.getItems(),
                "message", "商品已加入購物車"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("加入購物車失敗: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "加入購物車失敗"));
        }
    }

    @PutMapping("/{cartItemId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long cartItemId, 
                                           @RequestParam Integer quantity,
                                           HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        try {
            CartResponseDTO cart = cartService.updateCartItem(userId, cartItemId, quantity);
            return ResponseEntity.ok(Map.of("items", cart.getItems()));
        } catch (Exception e) {
            System.err.println("更新購物車失敗: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "更新失敗"));
        }
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long cartItemId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        
        try {
            cartService.removeCartItem(userId, cartItemId);
            return ResponseEntity.ok(Map.of("message", "商品已移除"));
        } catch (Exception e) {
            System.err.println("移除商品失敗: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "移除失敗"));
        }
    }
}
