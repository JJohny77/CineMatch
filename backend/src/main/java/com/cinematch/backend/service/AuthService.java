package com.cinematch.backend.service;

import com.cinematch.backend.auth.RegisterRequest;
import com.cinematch.backend.auth.Role;
import com.cinematch.backend.model.User;
import com.cinematch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void register(RegisterRequest request) {

        // 1. Έλεγχος unique email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // 2. Δημιουργία χρήστη με Default Role το USER
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        // 3. Αποθήκευση στο DB
        userRepository.save(user);
    }
}
