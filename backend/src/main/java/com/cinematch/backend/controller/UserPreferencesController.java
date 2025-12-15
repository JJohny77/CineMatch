package com.cinematch.backend.controller;

import com.cinematch.backend.dto.UserPreferencesResponseDto;
import com.cinematch.backend.model.User;
import com.cinematch.backend.service.CurrentUserService;
import com.cinematch.backend.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final CurrentUserService currentUserService;
    private final UserPreferenceService userPreferenceService;

    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferencesResponseDto> getMyPreferences(
            @RequestParam(name = "topN", defaultValue = "5") int topN
    ) {
        User user = currentUserService.getCurrentUserOrNull();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        int safeTopN = Math.max(1, Math.min(topN, 20));
        return ResponseEntity.ok(
                userPreferenceService.computeAndPersist(user, safeTopN)
        );
    }
}
