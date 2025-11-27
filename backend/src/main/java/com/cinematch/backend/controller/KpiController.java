package com.cinematch.backend.controller;

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

    @GetMapping("/star-power/{actorId}")
    public ResponseEntity<StarPowerResponse> getStarPower(@PathVariable Long actorId) {
        return ResponseEntity.ok(kpiService.calculateStarPower(actorId));
    }
}
