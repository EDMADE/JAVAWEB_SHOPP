package com.example.demo.repository;

import com.example.demo.model.entity.Favorite;
import com.example.demo.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
	   @Query("SELECT f FROM Favorite f " +
	           "JOIN FETCH f.product p " +
	           "LEFT JOIN FETCH p.skus " +
	           "WHERE f.userId = :userId " +
	           "ORDER BY f.createdAt DESC")
	    List<Favorite> findByUserIdWithProductOrderByCreatedAtDesc(@Param("userId") Long userId);


  
    boolean existsByUserIdAndProduct(Long userId, Product product);

    //刪除收藏
    @Modifying
    @Transactional
    void deleteByUserIdAndProduct(Long userId, Product product);

    
    long countByUserId(Long userId);

    
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Favorite f WHERE f.userId = :userId AND f.product.productId = :productId")
    boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Favorite f WHERE f.userId = :userId AND f.product.productId = :productId")
    void deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
