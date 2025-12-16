package com.cinematch.backend.controller.ai;

import com.cinematch.backend.model.ai.MovieSentiment;
import com.cinematch.backend.service.ai.MovieSentimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/movies")
@RequiredArgsConstructor
public class MovieSentimentController {

    private final MovieSentimentService movieSentimentService;

    /**
     * Admin / Dev only – batch sentiment analysis
     */
    @PostMapping("/{movieId}/analyze-sentiment")
    public ResponseEntity<?> analyzeSentiment(
            @PathVariable Long movieId,
            @RequestParam String text,
            @RequestParam(defaultValue = "overview") String source
    ) {
        MovieSentiment result =
                movieSentimentService.analyzeAndStore(movieId, text, source);

        if (result == null) {
            return ResponseEntity.ok("Sentiment already exists");
        }

        return ResponseEntity.ok(result);
    }
}
