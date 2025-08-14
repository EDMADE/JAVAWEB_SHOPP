package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.service.OrderService;
import com.example.demo.model.dto.OrderCreateDTO;
import com.example.demo.model.entity.Order;
import com.example.demo.model.entity.OrderItem; 
import com.example.demo.model.entity.Product;   
import com.example.demo.model.entity.User; 
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.OrderItemRepository; 
import com.example.demo.repository.ProductRepository;   
import com.example.demo.repository.UserRepository;    

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderRepository orderRepository;  
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("")
    public ResponseEntity<?> createOrder(
            @RequestBody OrderCreateDTO orderData,
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
            
            orderData.setUserId(userId);
            Order order = orderService.createOrder(orderData);
            
            if (order != null) {
                return ResponseEntity.ok(Map.of(
                    "message", "訂單創建成功",
                    "success", true,
                    "orderId", order.getOrderId(),
                    "orderNumber", "ORD" + String.format("%08d", order.getOrderId()),
                    "totalAmount", order.getTotalPrice()
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "訂單創建失敗", "success", false));
            }
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage(), "success", false));
        } catch (Exception e) {
            System.err.println("❌ 訂單創建失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤，請稍後再試", "success", false));
        }
     
    }
    

    @GetMapping("/seller-orders")
    public ResponseEntity<?> getSellerOrders(
            @RequestParam Long sellerId,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "請先登入", "success", false));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null || !userId.equals(sellerId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "無權限查看", "success", false));
            }
            
            // 根據是否有狀態參數決定查詢方式
            List<Order> sellerOrders;
            if (status != null && !status.trim().isEmpty()) {
                sellerOrders = orderRepository.findSellerOrdersByStatus(sellerId, status);
            } else {
                sellerOrders = orderRepository.findSellerOrders(sellerId);
            }
            
            List<Map<String, Object>> ordersWithItems = sellerOrders.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
                    User buyer = userRepository.findById(order.getUserId()).orElse(null);
                    
                    
                    List<Map<String, Object>> itemDetails = orderItems.stream()
                        .filter(item -> {
                            Product product = productRepository.findById(item.getProductId()).orElse(null);
                            return product != null && product.getSellerId().equals(sellerId);
                        })
                        .map(item -> {
                            Product product = productRepository.findById(item.getProductId()).orElse(null);
                            
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("productId", item.getProductId());
                            itemMap.put("productName", product != null ? product.getName() : "未知商品");
                            
                           
                            String imageUrl = product != null ? product.getMainImageUrl() : null;
                            if (imageUrl != null && !imageUrl.startsWith("/")) {
                                imageUrl = "/uploads/" + imageUrl;
                            }
                            itemMap.put("productImage", imageUrl);
                            itemMap.put("quantity", item.getQuantity());
                            itemMap.put("price", item.getPrice());
                            
                            return itemMap;
                        })
                        .collect(Collectors.toList());
                    
                    // 只有當訂單中包含賣家商品時才返回
                    if (!itemDetails.isEmpty()) {
                        Map<String, Object> orderMap = new HashMap<>();
                        orderMap.put("orderId", order.getOrderId());
                        orderMap.put("createdAt", order.getCreatedAt());
                        orderMap.put("status", order.getStatus());
                        orderMap.put("totalPrice", order.getTotalPrice());
                        orderMap.put("buyerName", buyer != null ? buyer.getUsername() : "未知買家");
                        orderMap.put("buyerPhone", order.getReceiverPhone());
                        orderMap.put("buyerAddress", order.getReceiverAddress());
                        orderMap.put("orderItems", itemDetails);
                        
                        return orderMap;
                    }
                    return null;
                })
                .filter(orderMap -> orderMap != null)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "orders", ordersWithItems,
                "success", true,
                "totalCount", ordersWithItems.size()
            ));
            
        } catch (Exception e) {
            System.err.println("❌ 獲取賣家訂單失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤", "success", false));
        }
    }

   
    @GetMapping("/seller-orders/statistics")
    public ResponseEntity<?> getSellerOrderStatistics(
            @RequestParam Long sellerId,
            HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "請先登入", "success", false));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null || !userId.equals(sellerId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "無權限查看", "success", false));
            }
            
            List<Object[]> statistics = orderRepository.countSellerOrdersByStatus(sellerId);
            
            Map<String, Long> statusCounts = new HashMap<>();
            for (Object[] stat : statistics) {
                String status = (String) stat[0];
                Long count = (Long) stat[1];
                statusCounts.put(status, count);
            }
            
            return ResponseEntity.ok(Map.of(
                "statistics", statusCounts,
                "success", true
            ));
            
        } catch (Exception e) {
            System.err.println("❌ 獲取賣家訂單統計失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤", "success", false));
        }
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
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
            
            String newStatus = request.get("status");
            if (newStatus == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "狀態不能為空", "success", false));
            }
            
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "訂單不存在", "success", false));
            }
            
            // 驗證是否為賣家的商品訂單
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
            boolean isSeller = orderItems.stream().anyMatch(item -> {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                return product != null && product.getSellerId().equals(userId);
            });
            
            if (!isSeller) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "無權限操作此訂單", "success", false));
            }
            
            order.setStatus(newStatus);
            orderRepository.save(order);
            
            return ResponseEntity.ok(Map.of(
                "message", "訂單狀態更新成功",
                "success", true
            ));
            
        } catch (Exception e) {
            System.err.println("❌ 更新訂單狀態失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤", "success", false));
        }
    }


    
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(HttpServletRequest httpRequest) {
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
            
            List<Order> orders = orderService.getOrdersByUserId(userId);
            
           
            List<Map<String, Object>> ordersWithItems = orders.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
                    
                    //組裝商品詳情
                    List<Map<String, Object>> itemDetails = orderItems.stream()
                        .map(item -> {
                            Product product = productRepository.findById(item.getProductId()).orElse(null);
                            User seller = null;
                            if (product != null) {
                                seller = userRepository.findById(product.getSellerId()).orElse(null);
                            }
                            
                            
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("productId", item.getProductId());
                            itemMap.put("productName", product != null ? product.getName() : "未知商品");
                            String imageUrl = product != null ? product.getMainImageUrl() : null;
                            if (imageUrl != null && (imageUrl.equals("/uploads/default.png") || 
                                    imageUrl.equals("default.png") || 
                                    imageUrl.contains("default.png"))) {
                imageUrl = null; 
            } else if (imageUrl != null && !imageUrl.startsWith("/")) {
                imageUrl = "/uploads/" + imageUrl;
            }
            itemMap.put("productImage", imageUrl);
                            itemMap.put("productImage", imageUrl);
                            itemMap.put("productImage", imageUrl);
                            itemMap.put("quantity", item.getQuantity());
                            itemMap.put("price", item.getPrice());
                            itemMap.put("sellerName", seller != null ? seller.getUsername() : "未知賣家");
                            itemMap.put("sellerId", product != null ? product.getSellerId() : null);
                            
                            return itemMap;
                        })
                        .collect(Collectors.toList());
                    
                 
                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("orderId", order.getOrderId());
                    orderMap.put("createdAt", order.getCreatedAt());
                    orderMap.put("status", order.getStatus());
                    orderMap.put("totalPrice", order.getTotalPrice());
                    orderMap.put("orderItems", itemDetails);
                    
                    return orderMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "orders", ordersWithItems,
                "success", true
            ));
            
        } catch (Exception e) {
            System.err.println("❌ 獲取訂單失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤", "success", false));
        }
    }
    
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
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
            
            // 驗證訂單是否屬於當前用戶
            Order order = orderService.getOrderByIdAndUserId(orderId, userId);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "訂單不存在或無權限操作", "success", false));
            }
            
            // 檢查訂單狀態是否可以取消
            if (!"PENDING".equals(order.getStatus())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "此訂單狀態無法取消", "success", false));
            }
            
            // 更新訂單狀態為已取消
            orderService.cancelOrder(orderId, userId);
            
            return ResponseEntity.ok(Map.of(
                "message", "訂單已取消",
                "success", true
            ));
            
        } catch (Exception e) {
            System.err.println("❌ 取消訂單失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統錯誤，請稍後再試", "success", false));
        }
    }
}
