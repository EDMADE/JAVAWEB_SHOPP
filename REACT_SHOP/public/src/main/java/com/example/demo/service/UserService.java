package com.example.demo.service;

import com.example.demo.exception.EmailAlreadyRegisteredException;
import com.example.demo.exception.UsernameAlreadyRegisteredException;
import com.example.demo.model.dto.UserRegisterDTO;
import com.example.demo.model.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;

    public Optional<User> findById(Long id) { return userRepository.findById(id); }
    public Optional<User> findByUsername(String username) { return userRepository.findByUsername(username); }
    public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }
    public List<User> findAll() { return userRepository.findAll(); }
    public boolean existsByUsername(String username) { return userRepository.existsByUsername(username); }
    public boolean existsByEmail(String email) { return userRepository.existsByEmail(email); }
    public User register(UserRegisterDTO dto) {
        return createUserWithEmailVerification(dto);
    }

    public User createUserWithEmailVerification(UserRegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new UsernameAlreadyRegisteredException("此用戶名已被使用");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyRegisteredException("此信箱已被註冊");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenCreatedAt(LocalDateTime.now());
        user.setTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        User savedUser = userRepository.save(user);

        System.out.println("✅ 註冊生成 token: " + token + " for " + savedUser.getUsername());
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getUsername(), token);
        return savedUser;
    }

    public String generateVerificationToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getVerificationToken() != null
                && user.getTokenExpiresAt() != null
                && user.getTokenExpiresAt().isAfter(now)) {
            System.out.println("ℹ Token 還有效，直接使用舊 token: " + user.getVerificationToken());
            return user.getVerificationToken();
        }
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenCreatedAt(now);
        user.setTokenExpiresAt(now.plusMinutes(30));
        userRepository.save(user);
        System.out.println("✅ 生成新 token: " + token + " for " + user.getUsername());
        return token;
    }

    public enum EmailVerificationResult {
        SUCCESS, ALREADY_VERIFIED, EXPIRED, INVALID
    }

    // 驗證 Email
    @Transactional
    public EmailVerificationResult verifyEmailToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return EmailVerificationResult.INVALID;
        }
        Optional<User> userOpt = userRepository.findByVerificationToken(token.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.isEmailVerified()) {
                return EmailVerificationResult.ALREADY_VERIFIED;
            }
            if (!user.isTokenValid()) {
                return EmailVerificationResult.EXPIRED;
            }
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            user.setTokenCreatedAt(null);
            user.setTokenExpiresAt(null);
            userRepository.save(user);
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
            return EmailVerificationResult.SUCCESS;
        }

        List<User> verifiedUsers = userRepository.findByEmailVerified(1);
        for (User u : verifiedUsers) {
            if (u.getVerificationToken() == null || u.getVerificationToken().isEmpty()) {
                return EmailVerificationResult.ALREADY_VERIFIED;
            }
        }

        return EmailVerificationResult.INVALID;
    }

    

    // 修改用戶名
    public User updateUsername(String currentUsername, String newUsername) {
        Optional<User> userOpt = userRepository.findByUsername(currentUsername);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUsername(newUsername);
            return userRepository.save(user);
        }
        return null;
    }

    // 修改密碼
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    // 重新寄送驗證信
    public String regenerateVerificationToken(String email) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.isEmailVerified()) {
                throw new IllegalStateException("此帳號已經驗證過了");
            }
            return generateVerificationToken(user);
        } else {
            throw new IllegalArgumentException("找不到此email");
        }
    }
}
