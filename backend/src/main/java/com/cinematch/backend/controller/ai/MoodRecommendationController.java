package com.cinematch.backend.controller.ai;

import com.cinematch.backend.dto.RecommendByMoodRequest;
import com.cinematch.backend.dto.RecommendByMoodResponse;
import com.cinematch.backend.service.ai.MoodRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class MoodRecommendationController {

    private final MoodRecommendationService moodRecommendationService;

    @PostMapping("/recommend-by-mood")
    public ResponseEntity<RecommendByMoodResponse> recommendByMood(@RequestBody RecommendByMoodRequest request) {
        RecommendByMoodResponse response =
                moodRecommendationService.recommendByMood(request.getText());
        return ResponseEntity.ok(response);
    }
}
