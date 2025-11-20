package com.cinematch.backend.controller;                     // Πακέτο controllers

import com.cinematch.backend.dto.UserProfileResponse;         // Το DTO που δημιουργήσαμε
import com.cinematch.backend.model.User;                      // Το User entity
import com.cinematch.backend.repository.UserRepository;       // Για να διαβάσουμε από DB
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;        // Για να πάρουμε το email από SecurityContext
import org.springframework.web.bind.annotation.*;

@RestController                                               // Controller REST
@RequestMapping("/user")                                      // Όλα τα endpoints ξεκινάνε από /user
@RequiredArgsConstructor                                      // Lombok: constructor injection
public class UserController {

    private final UserRepository userRepository;              // Inject UserRepository

    @GetMapping("/profile")                                   // GET /user/profile
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        // Το Spring Security βάζει το email σαν Principal στο Authentication Object
        String email = authentication.getName();              // Παίρνουμε το email του logged-in user

        // Φέρνουμε τον χρήστη από τη βάση με βάση το email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Δημιουργούμε το DTO response
        UserProfileResponse response = new UserProfileResponse(
                user.getEmail(),                              // email
                user.getRole().name(),                        // role ως String
                user.getQuizScore(),                          // quiz score
                user.getCreatedAt().toString()                // createdAt ως String
        );

        return ResponseEntity.ok(response);                   // Επιστροφή JSON
    }
}
