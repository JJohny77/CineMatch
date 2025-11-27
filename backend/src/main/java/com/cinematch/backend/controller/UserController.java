package com.cinematch.backend.controller;

import com.cinematch.backend.dto.UpdateProfileRequest;
import com.cinematch.backend.dto.UserProfileResponse;
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

    // ===========================================================
    //                  GET /user/profile
    // ===========================================================
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {

        String email = authentication.getName();  // email from JWT

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileResponse response = new UserProfileResponse(
                user.getEmail(),
                user.getUsername(),        // <-- now included
                user.getRole().name(),
                user.getQuizScore(),
                user.getCreatedAt().toString()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile/update")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        String email = authentication.getName();  // current logged in user

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email cannot be empty");
        }

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username cannot be empty");
        }

        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body("Email already in use");
            }
        }

        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());

        userRepository.save(user);

        return ResponseEntity.ok("Profile updated successfully");
    }
}
