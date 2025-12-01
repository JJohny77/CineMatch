package com.cinematch.backend.controller;

import com.cinematch.backend.dto.SentimentRequest;
import com.cinematch.backend.dto.SentimentResponse;
import com.cinematch.backend.service.ai.SentimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class SentimentController {

    private final SentimentService sentimentService;

    @PostMapping("/sentiment")
    public ResponseEntity<SentimentResponse> analyzeSentiment(@RequestBody SentimentRequest request) {
        SentimentResponse response = sentimentService.analyze(request.getText());
        return ResponseEntity.ok(response);
    }
}
