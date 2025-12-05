package com.cinematch.backend.controller.ai;

import com.cinematch.backend.service.ai.TMDBPersonBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/recast")
@RequiredArgsConstructor
public class RecastAdminController {

    private final TMDBPersonBatchService batchService;

    // Τελικό URL: POST http://localhost:8080/ai/recast/batch/full
    @PostMapping("/batch/full")
    public ResponseEntity<String> runFullBatch() {
        String msg = batchService.processAllPages();
        return ResponseEntity.ok(msg);
    }
}
