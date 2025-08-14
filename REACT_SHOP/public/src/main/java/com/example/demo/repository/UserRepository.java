package com.example.demo.repository;

import com.example.demo.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    //Email驗證查詢
    Optional<User> findByVerificationToken(String verificationToken);
    Optional<User> findByEmailVerifiedAndVerificationToken(Integer emailVerified, String verificationToken);
    List<User> findByEmailVerified(Integer emailVerified);
    long countByEmailVerified(Integer emailVerified);
    Optional<User> findByEmailAndEmailVerified(String email, Integer emailVerified);
    Optional<User> findByUsernameAndEmailVerified(String username, Integer emailVerified);
    List<User> findByTokenExpiresAtBefore(LocalDateTime dateTime);
    
    @Query("SELECT u FROM User u WHERE u.emailVerified = :verified")
    List<User> findByEmailVerifiedSafe(@Param("verified") Integer verified);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = :verified")
    long countByEmailVerifiedSafe(@Param("verified") Integer verified);
    
    
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.emailVerified = 1")
    Optional<User> findVerifiedUserByUsername(@Param("username") String username);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.emailVerified = 1")
    Optional<User> findVerifiedUserByEmail(@Param("email") String email);
    
   
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = 1, u.verificationToken = null, " +
           "u.tokenCreatedAt = null, u.tokenExpiresAt = null WHERE u.userId IN :userIds")
    int batchVerifyUsers(@Param("userIds") List<Long> userIds);
    
    @Modifying
    @Query("UPDATE User u SET u.verificationToken = null, u.tokenCreatedAt = null, " +
           "u.tokenExpiresAt = null WHERE u.tokenExpiresAt < :cutoffTime")
    int cleanupExpiredTokens(@Param("cutoffTime") LocalDateTime cutoffTime);
    @Query("SELECT u FROM User u WHERE u.emailVerified = 0 AND u.tokenExpiresAt > :currentTime")
    List<User> findUnverifiedUsersWithValidTokens(@Param("currentTime") LocalDateTime currentTime);
    @Query("SELECT u FROM User u WHERE u.emailVerified = 0 AND u.createdAt < :cutoffDate")
    List<User> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);
    @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = 1 AND u.createdAt BETWEEN :startDate AND :endDate")
    long countVerifiedUsersBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    List<User> findByTokenExpiresAtBeforeAndEmailVerified(LocalDateTime dateTime, Integer emailVerified);
    List<User> findByEmailVerifiedAndCreatedAtAfter(Integer emailVerified, LocalDateTime dateTime);
    List<User> findByUsernameContainingIgnoreCaseAndEmailVerified(String username, Integer emailVerified);
}
