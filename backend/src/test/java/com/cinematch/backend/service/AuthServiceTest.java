package com.cinematch.backend.service;

import com.cinematch.backend.auth.LoginRequest;
import com.cinematch.backend.auth.RegisterRequest;
import com.cinematch.backend.auth.Role;
import com.cinematch.backend.model.User;
import com.cinematch.backend.repository.UserRepository;
import com.cinematch.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(BCryptPasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);

        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@mail.com");
        request.setPassword("pass");

        when(userRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        authService.register(request);

        verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@mail.com");
        request.setPassword("1234");

        User user = User.builder()
                .email("john@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("fake-jwt");

        String token = authService.login(request);

        assertEquals("fake-jwt", token);
        verify(jwtUtil).generateToken(anyString(), anyString());
    }
}
