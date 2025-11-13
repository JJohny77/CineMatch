package com.cinematch.backend.controller;

import com.cinematch.backend.auth.RegisterRequest;
import com.cinematch.backend.model.User;
import com.cinematch.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.ok("User registered successfully");
    }
}
