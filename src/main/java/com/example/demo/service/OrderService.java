package com.example.demo.service;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ✅ 確保所有必要的import
import com.example.demo.model.dto.OrderCreateDTO;
import com.example.demo.model.dto.OrderItemDTO;
import com.example.demo.model.entity.CartItem;
import com.example.demo.model.entity.Order;
import com.example.demo.model.entity.OrderItem;
import com.example.demo.model.entity.Product;
import com.example.demo.model.entity.ProductSku;
import com.example.demo.model.entity.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSkuRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductSkuRepository productSkuRepository;
    
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Transactional
    public Order createOrder(OrderCreateDTO orderData) {
        try {
            System.out.println("🛒 開始創建訂單: " + orderData);
            
            if (!orderData.isValid()) {
                throw new IllegalArgumentException("訂單數據無效");
            }
            
            User buyer = userRepository.findById(orderData.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("用戶不存在"));
            
            if (!Boolean.TRUE.equals(buyer.getEmailVerified())) {
                throw new IllegalStateException("請先驗證email才能下單");
            }
            
            List<OrderItemDTO> orderItems = orderData.getOrderItems();
            BigDecimal calculatedTotal = BigDecimal.ZERO;
            
          
            for (OrderItemDTO itemDto : orderItems) {
                Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "商品不存在: " + itemDto.getProductId()));
                
                System.out.println("🔍 處理商品: ID=" + product.getProductId() + 
                                 ", 名稱=" + product.getName());
                
                int availableStock;
                String stockInfo;
                
               
                if (itemDto.getSkuId() != null) {
                    //檢查SKU庫存
                    ProductSku sku = productSkuRepository.findById(itemDto.getSkuId())
                        .orElseThrow(() -> new IllegalArgumentException(
                            "商品規格不存在: " + itemDto.getSkuId()));
                    
                    availableStock = sku.getStock();
                    stockInfo = "SKU庫存=" + availableStock + " (SKU ID: " + itemDto.getSkuId() + ")";
                } else {
                    //檢查商品庫存
                    availableStock = product.getStockQuantity();
                    stockInfo = "商品庫存=" + availableStock;
                }
                
                System.out.println("📦 " + stockInfo);
                
                if (availableStock < itemDto.getQuantity()) {
                    throw new IllegalArgumentException(
                        "商品 \"" + product.getName() + "\" 庫存不足，剩餘: " + 
                        availableStock + "，需要: " + itemDto.getQuantity());
                }
                
                BigDecimal itemTotal = itemDto.getPrice()
                    .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                calculatedTotal = calculatedTotal.add(itemTotal);
            }
            
            if (calculatedTotal.compareTo(orderData.getTotalAmount()) != 0) {
                throw new IllegalArgumentException("訂單總金額不正確");
            }
            
           
            Order order = new Order();
            order.setUserId(buyer.getUserId());
            order.setCreatedAt(LocalDateTime.now());
            order.setStatus("PENDING");
            order.setTotalPrice(calculatedTotal);
            
            OrderCreateDTO.ShippingInfoDTO shipping = orderData.getShippingInfo();
            order.setReceiverName(shipping.getName());
            order.setReceiverPhone(shipping.getPhone());
            order.setReceiverAddress(shipping.getAddress());
            order.setNote(shipping.getNote());
            
            Order savedOrder = orderRepository.save(order);
            System.out.println("✅ 訂單創建成功: " + savedOrder.getOrderId());
            
            //創建訂單項目並扣減庫存
            for (OrderItemDTO itemDto : orderItems) {
                Product product = productRepository.findById(itemDto.getProductId()).get();
                
                // 創建訂單項目
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(savedOrder.getOrderId());
                orderItem.setProductId(product.getProductId());
                orderItem.setSkuId(itemDto.getSkuId());
                orderItem.setQuantity(itemDto.getQuantity());
                orderItem.setPrice(itemDto.getPrice());
                
                orderItemRepository.save(orderItem);
                
             
                if (itemDto.getSkuId() != null) {
                    
                    ProductSku sku = productSkuRepository.findById(itemDto.getSkuId()).get();
                    int newSkuStock = sku.getStock() - itemDto.getQuantity();
                    sku.setStock(newSkuStock);
                    productSkuRepository.save(sku);
                    
                    System.out.println("📦 SKU庫存已更新: " + product.getName() + 
                                     " (SKU: " + sku.getSpecJson() + ")" +
                                     ", 購買數量: " + itemDto.getQuantity() +
                                     ", 剩餘庫存: " + newSkuStock);
                } else {
                   
                    int newStock = product.getStockQuantity() - itemDto.getQuantity();
                    product.setStockQuantity(newStock);
                    productRepository.save(product);
                    
                    System.out.println("📦 商品庫存已更新: " + product.getName() + 
                                     ", 購買數量: " + itemDto.getQuantity() +
                                     ", 剩餘庫存: " + newStock);
                }
                
                //發送賣家通知
                try {
                    User seller = userRepository.findById(product.getSellerId()).orElse(null);
                    if (seller != null && Boolean.TRUE.equals(seller.getEmailVerified())) {
                        emailService.sendNewOrderNotification(
                            seller, buyer, product, itemDto.getQuantity(), savedOrder);
                        System.out.println("📧 賣家通知已發送: " + seller.getEmail());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ 發送賣家通知失敗: " + e.getMessage());
                }
            }
            
            //清空該用戶的購物車
            try {
                List<CartItem> userCartItems = cartItemRepository.findByUserId(buyer.getUserId());
                if (!userCartItems.isEmpty()) {
                    cartItemRepository.deleteByUserId(buyer.getUserId());
                    System.out.println("🗑️ 已清空用戶購物車: " + userCartItems.size() + " 個項目");
                }
            } catch (Exception e) {
                System.err.println("⚠️ 清空購物車失敗: " + e.getMessage());
                
            }
            
            //發送買家確認信
            try {
                if (Boolean.TRUE.equals(buyer.getEmailVerified())) {
                    emailService.sendOrderConfirmation(buyer, savedOrder, orderItems);
                    System.out.println("📧 買家訂單確認信已發送: " + buyer.getEmail());
                }
            } catch (Exception e) {
                System.err.println("⚠️ 發送買家確認信失敗: " + e.getMessage());
            }
            
            return savedOrder;
            
        } catch (Exception e) {
            System.err.println("❌ 創建訂單失敗: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        try {
            //驗證訂單
            Order order = getOrderByIdAndUserId(orderId, userId);
            if (order == null) {
                throw new IllegalArgumentException("訂單不存在或無權限操作");
            }
            
            if (!"PENDING".equals(order.getStatus())) {
                throw new IllegalStateException("此訂單狀態無法取消");
            }
            
            //恢復商品庫存
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
            for (OrderItem item : orderItems) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    int newStock = product.getStockQuantity() + item.getQuantity();
                    product.setStockQuantity(newStock);
                    productRepository.save(product);
                    
                    System.out.println("📦 恢復商品庫存: " + product.getName() + 
                                     ", 恢復數量: " + item.getQuantity() +
                                     ", 新庫存: " + newStock);
                }
            }
            
            //更新訂單狀態
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            
            System.out.println("✅ 訂單已取消: " + orderId);
            
        } catch (Exception e) {
            System.err.println("❌ 取消訂單失敗: " + e.getMessage());
            throw e;
        }
    }
    
    public List<Order> getOrdersByUserId(Long userId) {
        try {
            return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } catch (Exception e) {
            System.err.println("查詢用戶訂單失敗: " + e.getMessage());
            return List.of();
        }
    }
    
    public Order getOrderByIdAndUserId(Long orderId, Long userId) {
        try {
            return orderRepository.findByOrderIdAndUserId(orderId, userId).orElse(null);
        } catch (Exception e) {
            System.err.println("查詢訂單失敗: " + e.getMessage());
            return null;
        }
}
}
