package com.cinematch.backend.controller;

import com.cinematch.backend.model.User;
import com.cinematch.backend.service.CurrentUserService;
import com.cinematch.backend.service.MovieRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieRecommendationController {

    private final CurrentUserService currentUserService;
    private final MovieRecommendationService movieRecommendationService;

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations() {
        User user = currentUserService.getCurrentUserOrNull();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(
                movieRecommendationService.getRecommendationsForUser(user)
        );
    }
}
