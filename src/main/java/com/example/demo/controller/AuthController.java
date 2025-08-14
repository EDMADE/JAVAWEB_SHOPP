package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import com.example.demo.exception.EmailAlreadyRegisteredException;
import com.example.demo.exception.UsernameAlreadyRegisteredException;
import com.example.demo.model.dto.LoginRequest;
import com.example.demo.model.dto.UserRegisterDTO;
import com.example.demo.model.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.service.EmailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private EmailService emailService;

    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterDTO dto) {
        try {
            if (userService.existsByUsername(dto.getUsername())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "此用戶名已被使用"));
            }
            if (userService.existsByEmail(dto.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "此信箱已被註冊"));
            }

            User user = userService.createUserWithEmailVerification(dto);

            return ResponseEntity.ok(Map.of(
                "message", "註冊成功，請檢查email進行驗證",
                "email", user.getEmail(),
                "requiresVerification", true
            ));
        } catch (EmailAlreadyRegisteredException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "此信箱已被註冊"));
        } catch (UsernameAlreadyRegisteredException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "此用戶名已被使用"));
        } catch (Exception e) {
            System.err.println("註冊失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "註冊失敗：" + e.getMessage()));
        }
    }


    
    @GetMapping("/verify-email/{token}")
    public ResponseEntity<?> verifyEmail(@PathVariable String token) {
        try {
            UserService.EmailVerificationResult result = userService.verifyEmailToken(token);
            switch (result) {
                case SUCCESS:
                    return ResponseEntity.ok(Map.of(
                        "message", "Email驗證成功，您現在可以登入了",
                        "verified", true,
                        "success", true
                    ));
                case ALREADY_VERIFIED:
                    return ResponseEntity.ok(Map.of(
                        "message", "您的Email已經驗證過，請直接登入",
                        "verified", true,
                        "success", true
                    ));
                case EXPIRED:
                    return ResponseEntity.badRequest().body(Map.of(
                        "message", "驗證鏈接已過期，請重新註冊或聯繫客服",
                        "verified", false,
                        "success", false
                    ));
                case INVALID:
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                        "message", "驗證鏈接無效，請確認連結或重新註冊",
                        "verified", false,
                        "success", false
                    ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "message", "系統錯誤，請稍後再試",
                    "verified", false,
                    "success", false
                ));
        }
    }

    
    
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            User user = userService.findByEmail(email).orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "找不到此email"));
            }

            if (user.isEmailVerified()) {
                return ResponseEntity.badRequest().body(Map.of("message", "此帳號已經驗證過了"));
            }

            String verificationToken = userService.generateVerificationToken(user);
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationToken);

            return ResponseEntity.ok(Map.of("message", "驗證郵件已重新發送"));

        } catch (Exception e) {
            System.err.println("重新發送驗證失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "發送失敗"));
        }
    }

   
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            
            User user = userService.findByUsername(request.getUsername()).orElse(null);
            if (user != null && !user.isEmailVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "message", "請先驗證您的email才能登入",
                        "success", false,
                        "requiresVerification", true,
                        "email", user.getEmail()
                    ));
            }
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            
            if (user != null) {
                session.setAttribute("userId", user.getUserId());
                
                System.out.println("登入成功 - 用戶ID: " + user.getUserId() + 
                                 ", Session ID: " + session.getId() + 
                                 ", 用戶名: " + user.getUsername());
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "登入成功");
                response.put("success", true);
                response.put("userId", user.getUserId());
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "登入過程中發生錯誤", "success", false));
            }
            
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "帳號或密碼錯誤", "success", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "登入失敗：" + e.getMessage(), "success", false));
        }
    }

    
    @GetMapping("/status")
    public ResponseEntity<?> getLoginStatus(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.ok(Map.of("loggedIn", false));
            }
            
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("loggedIn", false));
            }
            
            User user = userService.findById(userId).orElse(null);
            if (user != null && user.isEmailVerified()) {
                System.out.println("檢查登入狀態 - 用戶ID: " + userId + 
                                 ", Session ID: " + session.getId());
                
                Map<String, Object> response = new HashMap<>();
                response.put("loggedIn", true);
                response.put("userId", user.getUserId());
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                return ResponseEntity.ok(response);
            } else {
                session.invalidate();
                return ResponseEntity.ok(Map.of("loggedIn", false));
            }
        } catch (Exception e) {
            System.err.println("檢查登入狀態失敗: " + e.getMessage());
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Long userId = (Long) session.getAttribute("userId");
                System.out.println("用戶登出 - 用戶ID: " + userId + 
                                 ", Session ID: " + session.getId());
                session.invalidate();
            }
            
            SecurityContextHolder.clearContext();
            
            return ResponseEntity.ok(Map.of("message", "登出成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "登出成功"));
        }
    }
}
