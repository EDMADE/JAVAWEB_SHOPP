package com.example.demo.repository;

import com.example.demo.model.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    
    // 查詢某商品的最高出價
    @Query("SELECT b FROM Bid b WHERE b.productId = :productId ORDER BY b.amount DESC LIMIT 1")
    Optional<Bid> findHighestBidByProductId(@Param("productId") Long productId);
    
 
    List<Bid> findTop10ByProductIdOrderByBidTimeDesc(Long productId);
    
    // 查詢某商品的總出價次數
    long countByProductId(Long productId);
    
    // 查詢某商品的所有出價者 ID
    @Query("SELECT DISTINCT b.bidderId FROM Bid b WHERE b.productId = :productId")
    List<Long> findDistinctBiddersByProductId(@Param("productId") Long productId);
    

    List<Bid> findByBidderIdOrderByBidTimeDesc(Long bidderId);
}
