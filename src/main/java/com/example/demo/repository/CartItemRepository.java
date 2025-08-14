package com.example.demo.repository;

import com.example.demo.model.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    //查詢用戶的所有購物車項目
    List<CartItem> findByUserId(Long userId);

    
    void deleteByUserIdAndProductId(Long userId, Long productId);
    
    // 查詢特定用戶、商品、SKU 的項目
    @Query("SELECT c FROM CartItem c WHERE c.userId = :userId AND c.productId = :productId AND c.skuId = :skuId")
    Optional<CartItem> findByUserIdAndProductIdAndSkuId(
        @Param("userId") Long userId, 
        @Param("productId") Long productId, 
        @Param("skuId") Long skuId
    );
    
    //查詢特定用戶、商品、無 SKU 的項目
    @Query("SELECT c FROM CartItem c WHERE c.userId = :userId AND c.productId = :productId AND c.skuId IS NULL")
    Optional<CartItem> findByUserIdAndProductIdAndSkuIdIsNull(
        @Param("userId") Long userId, 
        @Param("productId") Long productId
    );
    
    //查詢是否存在相同的購物車項目（包含 NULL SKU 處理）
    @Query("SELECT c FROM CartItem c WHERE c.userId = :userId AND c.productId = :productId AND " +
           "(:skuId IS NULL AND c.skuId IS NULL OR c.skuId = :skuId)")
    Optional<CartItem> findExistingCartItem(
        @Param("userId") Long userId, 
        @Param("productId") Long productId, 
        @Param("skuId") Long skuId
    );
    
    //根據購物車項目 ID 和用戶 ID 查詢
    Optional<CartItem> findByCartItemIdAndUserId(Long cartItemId, Long userId);
    
    // 刪除特定用戶的特定項目
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.cartItemId = :cartItemId AND c.userId = :userId")
    int deleteByCartItemIdAndUserId(@Param("cartItemId") Long cartItemId, @Param("userId") Long userId);
    
    //檢查購物車項目是否存在
    @Query("SELECT COUNT(c) > 0 FROM CartItem c WHERE c.userId = :userId AND c.productId = :productId AND " +
           "(:skuId IS NULL AND c.skuId IS NULL OR c.skuId = :skuId)")
    boolean existsByUserIdAndProductIdAndSkuId(
        @Param("userId") Long userId, 
        @Param("productId") Long productId, 
        @Param("skuId") Long skuId
    );
    
    //清空用戶購物車
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
