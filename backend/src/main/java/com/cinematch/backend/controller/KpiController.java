package com.cinematch.backend.controller;

import com.cinematch.backend.dto.AudienceEngagementResponse;
import com.cinematch.backend.dto.StarPowerResponse;
import com.cinematch.backend.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    // ⭐ Star Power ανά ΤΑΙΝΙΑ
    @GetMapping("/star-power/movie/{movieId}")
    public ResponseEntity<StarPowerResponse> getStarPowerForMovie(
            @PathVariable Long movieId
    ) {
        return ResponseEntity.ok(kpiService.calculateStarPower(movieId));
    }

    // 🎭 Audience Engagement ανά ΤΑΙΝΙΑ
    @GetMapping("/audience-engagement/{movieId}")
    public ResponseEntity<AudienceEngagementResponse> getAudienceEngagement(
            @PathVariable Long movieId
    ) {
        return ResponseEntity.ok(kpiService.calculateAudienceEngagement(movieId));
    }
}
