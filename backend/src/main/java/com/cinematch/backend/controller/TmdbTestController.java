package com.cinematch.backend.controller;

import com.cinematch.backend.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tmdb")
public class TmdbTestController {

    private final TmdbService tmdbService;

    public TmdbTestController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(@RequestParam String query) {
        Map<String, String> params = Map.of(
                "query", query,
                "language", "en-US"
        );

        String result = tmdbService.fetchFromTmdb("/search/movie", params);
        return ResponseEntity.ok(result);
    }
}
