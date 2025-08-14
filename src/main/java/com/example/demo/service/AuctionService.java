package com.example.demo.service;

import com.example.demo.model.dto.BidDTO;
import com.example.demo.model.entity.Bid;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.User;
import com.example.demo.repository.BidRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuctionService {
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    
    @Transactional
    public BidDTO placeBid(Long productId, String username, BigDecimal amount) {
        try {
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
            
            //驗證是否為競標商品
            if (product.getBidEndTime() == null) {
                throw new IllegalStateException("此商品非競標商品");
            }
            
            
            if (LocalDateTime.now().isAfter(product.getBidEndTime())) {
                throw new IllegalStateException("競標已結束");
            }
            
           
            if (product.getStatus() != Product.ProductStatus.AUCTION) {
                throw new IllegalStateException("商品狀態不允許競標");
            }
            
         
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用戶不存在"));
            
            //防止賣家自己出價
            if (product.getSellerId().equals(user.getUserId())) {
                throw new IllegalStateException("賣家不能對自己的商品出價");
            }
            
            //取得目前最高出價
            Optional<Bid> currentHighestBid = bidRepository.findHighestBidByProductId(productId);
            BigDecimal currentHighestAmount = currentHighestBid
                .map(Bid::getAmount)
                .orElse(product.getStartPrice() != null ? product.getStartPrice() : BigDecimal.ZERO);
            
            //驗證出價金額（最少要比目前最高價多10元）
            BigDecimal minAmount = currentHighestAmount.add(new BigDecimal("10"));
            if (amount.compareTo(minAmount) < 0) {
                throw new IllegalArgumentException(
                    String.format("出價必須至少為 NT$ %,d", minAmount.intValue())
                );
            }
            
            // 檢查是否超過直購價
            if (product.getCurrentPrice() != null && 
                amount.compareTo(product.getCurrentPrice()) >= 0) {
                throw new IllegalStateException(
                    String.format("出價不能等於或超過直購價 NT$ %,d，請使用直購功能", 
                                product.getCurrentPrice().intValue())
                );
            }
            
            //檢查用戶是否已是最高出價者
            if (currentHighestBid.isPresent() && 
                currentHighestBid.get().getBidderId().equals(user.getUserId())) {
                throw new IllegalStateException("您已經是目前最高出價者");
            }
            
           
            Bid bid = new Bid();
            bid.setProductId(productId);
            bid.setBidderId(user.getUserId());
            bid.setAmount(amount);
            bid.setBidTime(LocalDateTime.now());
            
            Bid savedBid = bidRepository.save(bid);
            
           
            BidDTO result = new BidDTO();
            result.setProductId(productId);
            result.setBidderId(user.getUserId());
            result.setBidderName(user.getUsername());
            result.setAmount(amount);
            result.setBidTime(savedBid.getBidTime());
            result.setIsNewHighest(true);
            result.setMessage("出價成功！您現在是最高出價者");
            
            System.out.println(String.format("✅ 新出價: %s 對商品 %d 出價 NT$ %,d", 
                                            user.getUsername(), productId, amount.intValue()));
            
            return result;
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("❌ 出價失敗: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ 出價系統錯誤: " + e.getMessage());
            throw new RuntimeException("系統錯誤，請稍後再試");
        }
    }
    
   
    public Map<String, Object> getAuctionInfo(Long productId) {
        try {
            Product product = productRepository.findById(productId).orElse(null);
            
            //非競標商品
            if (product == null || product.getBidEndTime() == null) {
                return Map.of("isAuction", false);
            }
            
            //取得競標相關資料
            Optional<Bid> highestBid = bidRepository.findHighestBidByProductId(productId);
            List<Bid> recentBids = bidRepository.findTop10ByProductIdOrderByBidTimeDesc(productId);
            long bidCount = bidRepository.countByProductId(productId);
            
            //判斷競標是否結束
            boolean isEnded = LocalDateTime.now().isAfter(product.getBidEndTime());
            
            //取得目前最高價
            BigDecimal currentPrice = highestBid
                .map(Bid::getAmount)
                .orElse(product.getStartPrice() != null ? product.getStartPrice() : BigDecimal.ZERO);
            
            //組裝基本資訊
            Map<String, Object> result = new HashMap<>();
            result.put("isAuction", true);
            result.put("productId", productId);
            result.put("startPrice", product.getStartPrice());
            result.put("currentPrice", currentPrice);
            result.put("directBuyPrice", product.getCurrentPrice());
            result.put("endTime", product.getBidEndTime());
            result.put("isEnded", isEnded);
            result.put("bidCount", bidCount);
            
           
            if (highestBid.isPresent()) {
                User winner = userRepository.findById(highestBid.get().getBidderId()).orElse(null);
                result.put("currentWinner", winner != null ? winner.getUsername() : "匿名用戶");
                result.put("currentWinnerId", highestBid.get().getBidderId());
            } else {
                result.put("currentWinner", null);
                result.put("currentWinnerId", null);
            }
            
            
            List<Map<String, Object>> bidHistory = recentBids.stream()
                .map(bid -> {
                    User bidder = userRepository.findById(bid.getBidderId()).orElse(null);
                    Map<String, Object> bidInfo = new HashMap<>();
                    bidInfo.put("amount", bid.getAmount());
                    bidInfo.put("bidTime", bid.getBidTime());
                    bidInfo.put("bidderName", bidder != null ? 
                        maskUsername(bidder.getUsername()) : "匿名用戶");
                    bidInfo.put("bidderId", bid.getBidderId());
                    return bidInfo;
                })
                .collect(Collectors.toList());
            
            result.put("recentBids", bidHistory);
            
            // 計算下次最低出價金額
            BigDecimal nextMinBid = currentPrice.add(new BigDecimal("10"));
            result.put("nextMinBid", nextMinBid);
            
            // 競標狀態描述
            if (isEnded) {
                if (highestBid.isPresent()) {
                    result.put("statusMessage", "競標已結束，得標者為: " + 
                        (result.get("currentWinner")));
                } else {
                    result.put("statusMessage", "競標已結束，無人出價");
                }
            } else {
                long hoursLeft = java.time.Duration.between(
                    LocalDateTime.now(), product.getBidEndTime()).toHours();
                if (hoursLeft > 24) {
                    result.put("statusMessage", "競標進行中，剩餘 " + (hoursLeft / 24) + " 天");
                } else if (hoursLeft > 0) {
                    result.put("statusMessage", "競標進行中，剩餘 " + hoursLeft + " 小時");
                } else {
                    long minutesLeft = java.time.Duration.between(
                        LocalDateTime.now(), product.getBidEndTime()).toMinutes();
                    result.put("statusMessage", "競標即將結束，剩餘 " + Math.max(0, minutesLeft) + " 分鐘");
                }
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 取得競標資訊失敗: " + e.getMessage());
            return Map.of("isAuction", false, "error", "載入競標資訊失敗");
        }
    }
    
    //直購功能
    @Transactional
    public Map<String, Object> buyNow(Long productId, String username) {
        try {
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
            
            
            if (product.getBidEndTime() == null) {
                throw new IllegalStateException("此商品非競標商品");
            }
            
           
            if (LocalDateTime.now().isAfter(product.getBidEndTime())) {
                throw new IllegalStateException("競標已結束，無法使用直購");
            }
            
           
            if (product.getCurrentPrice() == null || 
                product.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("此商品沒有設定直購價");
            }
            
            //驗證用戶存在
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用戶不存在"));
            
            
            if (product.getSellerId().equals(user.getUserId())) {
                throw new IllegalStateException("賣家不能直購自己的商品");
            }
            
           
            Bid directBuyBid = new Bid();
            directBuyBid.setProductId(productId);
            directBuyBid.setBidderId(user.getUserId());
            directBuyBid.setAmount(product.getCurrentPrice());
            directBuyBid.setBidTime(LocalDateTime.now());
            bidRepository.save(directBuyBid);
            
           
            product.setBidEndTime(LocalDateTime.now());
            product.setStatus(Product.ProductStatus.INACTIVE); 
            productRepository.save(product);
            
          
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "直購成功！競標已結束");
            result.put("productId", productId);
            result.put("buyerName", username);
            result.put("buyerId", user.getUserId());
            result.put("price", product.getCurrentPrice());
            result.put("purchaseTime", LocalDateTime.now());
            
            System.out.println(String.format("✅ 直購成功: %s 直購商品 %d，價格 NT$ %,d", 
                                            username, productId, product.getCurrentPrice().intValue()));
            
            return result;
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("❌ 直購失敗: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ 直購系統錯誤: " + e.getMessage());
            throw new RuntimeException("系統錯誤，請稍後再試");
        }
    }
    
    //動處理過期競標
    @Transactional
    public void processExpiredAuctions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            
            List<Product> expiredAuctions = productRepository.findAll().stream()
                .filter(p -> p.getStatus() == Product.ProductStatus.AUCTION)
                .filter(p -> p.getBidEndTime() != null && now.isAfter(p.getBidEndTime()))
                .collect(Collectors.toList());
            
            for (Product product : expiredAuctions) {
                try {
                    //更新商品狀態
                    product.setStatus(Product.ProductStatus.INACTIVE);
                    productRepository.save(product);
                    
                    //取得得標者資訊
                    Optional<Bid> winningBid = bidRepository.findHighestBidByProductId(product.getProductId());
                    if (winningBid.isPresent()) {
                        User winner = userRepository.findById(winningBid.get().getBidderId()).orElse(null);
                        System.out.println(String.format("🏆 競標結束: 商品 %s (ID: %d)，得標者: %s，得標價: NT$ %,d",
                            product.getName(), product.getProductId(),
                            winner != null ? winner.getUsername() : "未知用戶",
                            winningBid.get().getAmount().intValue()));
                    } else {
                        System.out.println(String.format("📦 競標結束: 商品 %s (ID: %d)，無人出價",
                            product.getName(), product.getProductId()));
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ 處理過期競標失敗: 商品ID " + product.getProductId() + 
                                     ", 錯誤: " + e.getMessage());
                }
            }
            
            if (!expiredAuctions.isEmpty()) {
                System.out.println(String.format("✅ 處理了 %d 個過期競標", expiredAuctions.size()));
            }
            
        } catch (Exception e) {
            System.err.println("❌ 處理過期競標系統錯誤: " + e.getMessage());
        }
    }
    
    //取得用戶的競標記錄
    public List<Map<String, Object>> getUserBids(String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return new ArrayList<>();
            }
            
            List<Bid> userBids = bidRepository.findByBidderIdOrderByBidTimeDesc(user.getUserId());
            
            return userBids.stream()
                .map(bid -> {
                    Product product = productRepository.findById(bid.getProductId()).orElse(null);
                    Map<String, Object> bidInfo = new HashMap<>();
                    bidInfo.put("bidId", bid.getBidId());
                    bidInfo.put("productId", bid.getProductId());
                    bidInfo.put("productName", product != null ? product.getName() : "未知商品");
                    bidInfo.put("amount", bid.getAmount());
                    bidInfo.put("bidTime", bid.getBidTime());
                    
                    //判斷是否為最高出價
                    Optional<Bid> highestBid = bidRepository.findHighestBidByProductId(bid.getProductId());
                    bidInfo.put("isHighest", highestBid.isPresent() && 
                                           highestBid.get().getBidId().equals(bid.getBidId()));
                    
                    //競標狀態
                    if (product != null && product.getBidEndTime() != null) {
                        boolean isEnded = LocalDateTime.now().isAfter(product.getBidEndTime());
                        bidInfo.put("auctionEnded", isEnded);
                        if (isEnded && bidInfo.get("isHighest").equals(true)) {
                            bidInfo.put("status", "得標");
                        } else if (isEnded) {
                            bidInfo.put("status", "未得標");
                        } else {
                            bidInfo.put("status", bidInfo.get("isHighest").equals(true) ? "領先中" : "被超越");
                        }
                    }
                    
                    return bidInfo;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("❌ 取得用戶競標記錄失敗: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
  
    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return username;
        }
        
        if (username.length() <= 4) {
            return username.substring(0, 1) + "*".repeat(username.length() - 2) + 
                   username.substring(username.length() - 1);
        }
        
        return username.substring(0, 2) + "*".repeat(username.length() - 4) + 
               username.substring(username.length() - 2);
    }
    
    //檢查用戶是否為商品的最高出價者
    public boolean isHighestBidder(Long productId, String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) return false;
            
            Optional<Bid> highestBid = bidRepository.findHighestBidByProductId(productId);
            return highestBid.isPresent() && 
                   highestBid.get().getBidderId().equals(user.getUserId());
                   
        } catch (Exception e) {
            return false;
        }
    }
}
