package com.example.demo.repository;

import com.example.demo.model.dto.ProductDTO;
import com.example.demo.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
   
    List<Product> findByCategory(String category);
    List<Product> findBySellerId(Long sellerId);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByStatus(Product.ProductStatus status);
    List<Product> findBySellerIdAndCategory(Long sellerId, String category);
    
    //查詢上架商品（不包含競標結束的）
    @Query("SELECT p FROM Product p WHERE p.status IN ('ACTIVE', 'AUCTION') AND " +
           "(p.bidEndTime IS NULL OR p.bidEndTime > CURRENT_TIMESTAMP)")
    List<Product> findAllActiveProducts();
    
    //查詢競標結束的商品（用於自動下架）
    @Query("SELECT p FROM Product p WHERE p.status = 'AUCTION' AND p.bidEndTime <= :currentTime")
    List<Product> findExpiredAuctions(@Param("currentTime") LocalDateTime currentTime);
    
    
    @Query("SELECT new com.example.demo.model.dto.ProductDTO(" +
           "p.id, p.sellerId, p.name, p.category, p.description, p.startPrice, " +
           "p.currentPrice, p.stockQuantity, p.mainImageUrl, p.bidEndTime, " +
           "CAST(p.status AS string), p.createdAt, p.specifications, p.productCondition, " +
           "COALESCE(MIN(sku.price), p.currentPrice), " +
           "COALESCE(MAX(sku.price), p.currentPrice), " +
           "COALESCE(SUM(sku.stock), p.stockQuantity)) " +
           "FROM Product p LEFT JOIN ProductSku sku ON sku.productId = p.id " +
           "WHERE p.status IN ('ACTIVE', 'AUCTION') AND " +
           "(p.bidEndTime IS NULL OR p.bidEndTime > CURRENT_TIMESTAMP) " +
           "GROUP BY p.id, p.sellerId, p.name, p.category, p.description, p.startPrice, " +
           "p.currentPrice, p.stockQuantity, p.mainImageUrl, p.bidEndTime, " +
           "p.status, p.createdAt, p.specifications, p.productCondition " +
           "ORDER BY p.createdAt DESC")
    List<ProductDTO> findActiveProductsWithSkuData();

  
    @Query("SELECT new com.example.demo.model.dto.ProductDTO(" +
           "p.id, p.sellerId, p.name, p.category, p.description, p.startPrice, " +
           "p.currentPrice, p.stockQuantity, p.mainImageUrl, p.bidEndTime, " +
           "CAST(p.status AS string), p.createdAt, p.specifications, p.productCondition, " +
           "COALESCE(MIN(sku.price), p.currentPrice), " +
           "COALESCE(MAX(sku.price), p.currentPrice), " +
           "COALESCE(SUM(sku.stock), p.stockQuantity)) " +
           "FROM Product p LEFT JOIN ProductSku sku ON sku.productId = p.id " +
           "WHERE p.sellerId = :userId " +
           "GROUP BY p.id, p.sellerId, p.name, p.category, p.description, p.startPrice, " +
           "p.currentPrice, p.stockQuantity, p.mainImageUrl, p.bidEndTime, " +
           "p.status, p.createdAt, p.specifications, p.productCondition " +
           "ORDER BY p.createdAt DESC")
    List<ProductDTO> findDTOsBySellerIdWithSkuData(@Param("userId") Long userId);

 
    @Query("SELECT new com.example.demo.model.dto.ProductDTO(" +
           "p.id, p.sellerId, p.name, p.category, p.description, p.startPrice, " +
           "p.currentPrice, p.stockQuantity, p.mainImageUrl, p.bidEndTime, " +
           "CAST(p.status AS string), p.createdAt, p.specifications, p.productCondition) " +
           "FROM Product p WHERE p.status IN ('ACTIVE', 'AUCTION') AND " +
           "(p.bidEndTime IS NULL OR p.bidEndTime > CURRENT_TIMESTAMP) " +
           "ORDER BY p.createdAt DESC")
    List<ProductDTO> findActiveProducts();

    //查詢用戶商品
    @Query("SELECT new com.example.demo.model.dto.ProductDTO(" +
           "p.id, p.sellerId, p.name, p.category, p.description, p.startPrice, " +
           "p.currentPrice, p.stockQuantity, p.mainImageUrl, p.bidEndTime, " +
           "CAST(p.status AS string), p.createdAt, p.specifications, p.productCondition) " +
           "FROM Product p WHERE p.sellerId = :userId " +
           "ORDER BY p.createdAt DESC")
    List<ProductDTO> findDTOsBySellerId(@Param("userId") Long userId);
    
    //分類商品查詢
    @Query("SELECT new com.example.demo.model.dto.ProductDTO(" +
           "p.id, p.sellerId, p.name, p.category, p.description, p.startPrice, " +
           "p.currentPrice, p.stockQuantity, p.mainImageUrl, p.bidEndTime, " +
           "CAST(p.status AS string), p.createdAt, p.specifications, p.productCondition) " +
           "FROM Product p WHERE p.category = :category AND " +
           "p.status IN ('ACTIVE', 'AUCTION') AND " +
           "(p.bidEndTime IS NULL OR p.bidEndTime > CURRENT_TIMESTAMP) " +
           "ORDER BY p.createdAt DESC")
    List<ProductDTO> findActiveByCategoryWithDto(@Param("category") String category);
    
    //搜尋商品
    @Query("SELECT new com.example.demo.model.dto.ProductDTO(" +
           "p.id, p.sellerId, p.name, p.category, p.description, p.startPrice, " +
           "p.currentPrice, p.stockQuantity, p.mainImageUrl, p.bidEndTime, " +
           "CAST(p.status AS string), p.createdAt, p.specifications, p.productCondition) " +
           "FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
           "p.status IN ('ACTIVE', 'AUCTION') AND " +
           "(p.bidEndTime IS NULL OR p.bidEndTime > CURRENT_TIMESTAMP) " +
           "ORDER BY p.createdAt DESC")
    List<ProductDTO> searchActiveProductsWithDto(@Param("keyword") String keyword);
}
