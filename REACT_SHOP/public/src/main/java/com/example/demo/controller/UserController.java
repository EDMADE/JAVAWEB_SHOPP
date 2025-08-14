package com.example.demo.controller;

import com.example.demo.model.entity.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 取得公共資訊
    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicUserInfo(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(Map.of(
                        "userId", user.getUserId(),
                        "username", user.getUsername()
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "userId", id,
                        "username", "用戶" + id
                )));
    }

    // 取得當前用戶資訊
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入", "message", "請先登入"));
        }
        String username = auth.getName();
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(Map.of(
                        "userId", user.getUserId(),
                        "username", user.getUsername(),
                        "email", user.getEmail()
                )))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "用戶不存在")));
    }

    // 更新用戶名
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(Authentication auth, @RequestBody Map<String, String> updateData) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入", "message", "請先登入"));
        }
        String username = auth.getName();
        String newUsername = updateData.get("username");
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用戶名稱不能為空"));
        }
        User updatedUser = userService.updateUsername(username, newUsername);
        if (updatedUser != null) {
            return ResponseEntity.ok(Map.of(
                    "userId", updatedUser.getUserId(),
                    "username", updatedUser.getUsername(),
                    "email", updatedUser.getEmail()
            ));
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "用戶不存在"));
        }
    }

    // 修改密碼
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody Map<String, String> passwordData) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入", "message", "請先登入"));
        }
        String username = auth.getName();
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "請提供舊密碼和新密碼"));
        }
        boolean success = userService.changePassword(username, oldPassword, newPassword);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "密碼已更新"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "舊密碼錯誤"));
        }
    }

    // 取得所有用戶清單
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }
}
