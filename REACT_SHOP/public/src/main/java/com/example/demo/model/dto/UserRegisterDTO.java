package com.example.demo.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegisterDTO {
    @NotBlank(message = "用戶名不能為空")
    private String username;
    
    @NotBlank(message = "密碼不能為空")
    private String password;
    
    @NotBlank(message = "郵箱不能為空")
    private String email;
}
