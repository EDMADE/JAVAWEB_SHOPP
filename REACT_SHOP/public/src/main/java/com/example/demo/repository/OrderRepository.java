package com.example.demo.repository;

import com.example.demo.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByOrderIdAndUserId(Long orderId, Long userId);
    List<Order> findByStatus(String status);
    
    //查詢賣家的所有訂單
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN OrderItem oi ON o.orderId = oi.orderId " +
           "JOIN Product p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findSellerOrders(@Param("sellerId") Long sellerId);
    
    //查詢賣家特定狀態的訂單
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN OrderItem oi ON o.orderId = oi.orderId " +
           "JOIN Product p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId AND o.status = :status " +
           "ORDER BY o.createdAt DESC")
    List<Order> findSellerOrdersByStatus(@Param("sellerId") Long sellerId, @Param("status") String status);
    
    //查詢賣家的待處理訂單
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN OrderItem oi ON o.orderId = oi.orderId " +
           "JOIN Product p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId AND o.status = 'PENDING' " +
           "ORDER BY o.createdAt DESC")
    List<Order> findSellerPendingOrders(@Param("sellerId") Long sellerId);
    
    //統計賣家各狀態訂單數量
    @Query("SELECT o.status, COUNT(DISTINCT o.orderId) FROM Order o " +
           "JOIN OrderItem oi ON o.orderId = oi.orderId " +
           "JOIN Product p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId " +
           "GROUP BY o.status")
    List<Object[]> countSellerOrdersByStatus(@Param("sellerId") Long sellerId);
}
