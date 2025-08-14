package com.example.demo.exception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailRegistered(EmailAlreadyRegisteredException ex) {
        ApiError error = new ApiError("EMAIL_ALREADY_REGISTERED", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT); 
    }

    @ExceptionHandler(UsernameAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleUsernameRegistered(UsernameAlreadyRegisteredException ex) {
        ApiError error = new ApiError("USERNAME_ALREADY_REGISTERED", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT); 
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        
        response.put("errors", errors);
        response.put("message", "資料驗證失敗");
        return response;
    }
   
}
