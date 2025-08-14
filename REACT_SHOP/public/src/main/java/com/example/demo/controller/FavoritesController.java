package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.entity.Favorite;
import com.example.demo.model.entity.Product;
import com.example.demo.repository.FavoriteRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/favorites")
public class FavoritesController {
    
    @Autowired
    private FavoriteRepository favoriteRepository;
    
    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/{productId}")
    public ResponseEntity<?> addFavorite(
            @PathVariable Long productId,
            HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "請先登入", "success", false));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "請先登入", "success", false));
            }
            
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "商品不存在", "success", false));
            }
            
            if (product.getSellerId().equals(userId)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "不能收藏自己的商品", "success", false));
            }
            
            if (favoriteRepository.existsByUserIdAndProduct(userId, product)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "商品已在收藏列表中", "success", false));
            }
            
            
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setProduct(product);
            favoriteRepository.save(favorite);
            
            System.out.println("✅ 收藏成功：用戶 " + userId + " 收藏商品 " + productId);
            
            return ResponseEntity.ok(Map.of(
                "message", "收藏成功",
                "success", true,
                "isFavorited", true
            ));
            
        } catch (Exception e) {
            System.err.println("❌ 添加收藏失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤，請稍後再試", "success", false));
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFavorite(
            @PathVariable Long productId,
            HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "請先登入", "success", false));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "請先登入", "success", false));
            }
            
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "商品不存在", "success", false));
            }
            
            
            favoriteRepository.deleteByUserIdAndProduct(userId, product);
            
            return ResponseEntity.ok(Map.of(
                "message", "已移除收藏",
                "success", true,
                "isFavorited", false
            ));
            
        } catch (Exception e) {
            System.err.println("❌ 移除收藏失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤，請稍後再試", "success", false));
        }
    }

    @GetMapping("/my-favorites")
    public ResponseEntity<?> getMyFavorites(HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "請先登入", "success", false));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "請先登入", "success", false));
            }
            
            System.out.println("🔍 開始查詢用戶 " + userId + " 的收藏");
            
            List<Favorite> favorites = favoriteRepository.findByUserIdWithProductOrderByCreatedAtDesc(userId);
            System.out.println("📊 查詢到 " + favorites.size() + " 個收藏");
            
            List<Map<String, Object>> favoriteProducts = favorites.stream()
                .filter(favorite -> favorite.getProduct() != null)
                .map(favorite -> {
                    Product product = favorite.getProduct();
                    System.out.println("📦 處理商品: " + product.getName());
                    System.out.println("📸 商品圖片: " + product.getMainImageUrl());
                    
                    Map<String, Object> favoriteMap = new HashMap<>();
                    favoriteMap.put("favoriteId", favorite.getFavoriteId());
                    favoriteMap.put("productId", product.getProductId());
                    favoriteMap.put("productName", product.getName());
                    favoriteMap.put("productImage", product.getMainImageUrl());
                    
                    BigDecimal price = product.getCurrentPrice();
                    if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
                        price = product.getStartPrice();
                    }
                    favoriteMap.put("price", price != null ? price : BigDecimal.ZERO);
                    favoriteMap.put("createdAt", favorite.getCreatedAt());
                    
                    System.out.println("✅ 商品處理完成: " + favoriteMap);
                    return favoriteMap;
                })
                .collect(Collectors.toList());
            
            System.out.println("📋 最終回傳 " + favoriteProducts.size() + " 個收藏商品");
            
            return ResponseEntity.ok(Map.of(
                "favorites", favoriteProducts,
                "success", true
            ));
            
        } catch (Exception e) {
            System.err.println("❌ 獲取收藏失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤", "success", false));
        }
    }

    @GetMapping("/{productId}/status")
    public ResponseEntity<?> getFavoriteStatus(
            @PathVariable Long productId,
            HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.ok(Map.of("isFavorited", false, "success", true));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("isFavorited", false, "success", true));
            }
            
            Product product = productRepository.findById(productId).orElse(null);
            boolean isFavorited = product != null && favoriteRepository.existsByUserIdAndProduct(userId, product);
            
            return ResponseEntity.ok(Map.of("isFavorited", isFavorited, "success", true));
            
        } catch (Exception e) {
            System.err.println("❌ 檢查收藏狀態失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤", "success", false));
        }
    }
}
