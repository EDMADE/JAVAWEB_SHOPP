package com.example.demo.repository;

import com.example.demo.model.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    
    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);
    
    int countByProductId(Long productId);
    
    @Modifying
    @Transactional
    void deleteByProductId(Long productId);
    
    boolean existsByProductId(Long productId);
}
