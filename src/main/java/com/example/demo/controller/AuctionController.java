package com.example.demo.controller;

import com.example.demo.model.dto.BidDTO;
import com.example.demo.model.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/auction")
public class AuctionController {
    
    @Autowired
    private AuctionService auctionService;
    
    @Autowired
    private UserRepository userRepository;
    
   
    @GetMapping("/{productId}")
    public ResponseEntity<?> getAuctionInfo(@PathVariable Long productId) {
        try {
            System.out.println("📋 查詢競標資訊，商品ID: " + productId);
            
            Map<String, Object> result = auctionService.getAuctionInfo(productId);
            
            System.out.println("✅ 競標資訊查詢成功: " + result.get("isAuction"));
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 商品不存在: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("❌ 查詢競標資訊失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
   
    @PostMapping("/{productId}/bid")
    public ResponseEntity<?> placeBid(@PathVariable Long productId, 
                                     @RequestBody Map<String, Object> bidData,
                                     HttpServletRequest request) {
        try {
            //session驗證登入狀態
            HttpSession session = request.getSession(false);
            if (session == null) {
                System.out.println("🚫 未登入用戶嘗試出價，商品ID: " + productId);
                return ResponseEntity.status(401).body(Map.of("error", "請先登入"));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "請先登入"));
            }
            
            //用戶資訊
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "用戶不存在"));
            }
            
            String username = user.getUsername();
            System.out.println("💰 用戶出價: " + username + "，商品ID: " + productId);
            
           
            if (!bidData.containsKey("amount")) {
                return ResponseEntity.badRequest().body(Map.of("error", "請提供出價金額"));
            }
            
            BigDecimal amount;
            try {
                Object amountObj = bidData.get("amount");
                if (amountObj instanceof String) {
                    amount = new BigDecimal((String) amountObj);
                } else if (amountObj instanceof Number) {
                    amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "出價金額格式錯誤"));
                }
                
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "出價金額必須大於0"));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "出價金額格式錯誤"));
            }
            
           
            BidDTO result = auctionService.placeBid(productId, username, amount);
            
            System.out.println("✅ 出價成功: " + username + " 出價 NT$ " + amount);
            return ResponseEntity.ok(Map.of(
                "message", "出價成功",
                "bidData", result,
                "newPrice", amount,
                "bidder", username
            ));
            
        } catch (IllegalStateException e) {
            System.err.println("❌ 出價狀態錯誤: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 出價參數錯誤: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ 出價系統錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "系統錯誤，請稍後再試"));
        }
    }
    
   
    @PostMapping("/{productId}/buy-now")
    public ResponseEntity<?> buyNow(@PathVariable Long productId, HttpServletRequest request) {
        try {
            
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(401).body(Map.of("error", "請先登入"));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "請先登入"));
            }
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "用戶不存在"));
            }
            
            String username = user.getUsername();
            System.out.println("💳 用戶直購: " + username + "，商品ID: " + productId);
            
           
            Map<String, Object> result = auctionService.buyNow(productId, username);
            
            System.out.println("✅ 直購成功: " + username);
            return ResponseEntity.ok(result);
            
        } catch (IllegalStateException e) {
            System.err.println("❌ 直購狀態錯誤: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 直購參數錯誤: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ 直購系統錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "系統錯誤，請稍後再試"));
        }
    }
    
    
    @GetMapping("/my-bids")
    public ResponseEntity<?> getMyBids(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(401).body(Map.of("error", "請先登入"));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "請先登入"));
            }
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "用戶不存在"));
            }
            
            var bidHistory = auctionService.getUserBids(user.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "bids", bidHistory,
                "success", true
            ));
            
        } catch (Exception e) {
            System.err.println("❌ 獲取競標記錄失敗: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "系統錯誤"));
        }
    }
}
