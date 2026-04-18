package com.chatbotsaas.chatbot_saas.auth.controller;

import com.chatbotsaas.chatbot_saas.auth.dto.ChangePasswordRequest;
import com.chatbotsaas.chatbot_saas.auth.dto.LoginRequest;
import com.chatbotsaas.chatbot_saas.auth.dto.LoginResponse;
import com.chatbotsaas.chatbot_saas.auth.dto.RegisterRequest;
import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        authService.register(registerRequest);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PutMapping("/change_password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest
    ) {
        authService.changePassword(changePasswordRequest);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
