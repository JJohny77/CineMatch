package com.cinematch.backend.controller;

import com.cinematch.backend.dto.MovieSearchResponse;
import com.cinematch.backend.service.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MovieController {

    private final TmdbService tmdbService;

    /**
     * US11 — Search movies endpoint
     * Example call:
     * GET /movies/search?query=matrix
     */
    @GetMapping("/movies/search")
    public MovieSearchResponse searchMovies(@RequestParam String query) {
        return tmdbService.searchMovies(query);
    }
}
