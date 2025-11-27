package com.cinematch.backend.controller;

import com.cinematch.backend.dto.UserProfileResponse;
import com.cinematch.backend.dto.UserUpdateRequest;
import com.cinematch.backend.model.User;
import com.cinematch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileResponse response = new UserProfileResponse(
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getQuizScore(),
                user.getCreatedAt().toString()
        );

        return ResponseEntity.ok(response);
    }


    @PutMapping("/update")
    public ResponseEntity<UserProfileResponse> updateUser(
            Authentication authentication,
            @RequestBody UserUpdateRequest request
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));


        user.setUsername(request.getUsername());


        userRepository.save(user);


        UserProfileResponse response = new UserProfileResponse(
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getQuizScore(),
                user.getCreatedAt().toString()
        );

        return ResponseEntity.ok(response);
    }
}
