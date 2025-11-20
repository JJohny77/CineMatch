package com.cinematch.backend.service;

import com.cinematch.backend.auth.LoginRequest;
import com.cinematch.backend.auth.RegisterRequest;
import com.cinematch.backend.auth.Role;
import com.cinematch.backend.model.User;
import com.cinematch.backend.repository.UserRepository;
import com.cinematch.backend.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }

    // 🔥 ΝΕΑ ΜΕΘΟΔΟΣ LOGIN
    public String login(LoginRequest request) {

        // 1. Βρίσκουμε τον χρήστη
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // 2. Έλεγχος password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // 3. Δημιουργία JWT token
        return jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
