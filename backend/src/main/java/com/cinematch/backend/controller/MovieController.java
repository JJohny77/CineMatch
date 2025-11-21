package com.cinematch.backend.controller;

import com.cinematch.backend.dto.MovieDetailsDto;
import com.cinematch.backend.dto.MovieSearchResponse;
import com.cinematch.backend.dto.TrendingMovieDto;
import com.cinematch.backend.dto.MovieVideoDto;
import com.cinematch.backend.service.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final TmdbService tmdbService;

    /**
     * US11 – Search movies
     * GET /movies/search?query=matrix
     */
    @GetMapping("/search")
    public MovieSearchResponse searchMovies(@RequestParam String query) {
        return tmdbService.searchMovies(query);
    }

    /**
     * US12 – Trending movies
     * GET /movies/trending?time_window=day
     */
    @GetMapping("/trending")
    public ResponseEntity<List<TrendingMovieDto>> getTrending(
            @RequestParam(defaultValue = "day") String time_window
    ) {
        return ResponseEntity.ok(
                tmdbService.getTrendingMovies(time_window)
        );
    }

    /**
     * US18 – Movie Videos (trailers)
     * GET /movies/{id}/videos
     * 👉 ΠΡΕΠΕΙ ΝΑ ΜΠΕΙ ΠΑΝΩ ΑΠΟ ΤΟ /movies/{id}
     */
    @GetMapping("/{id}/videos")
    public ResponseEntity<List<MovieVideoDto>> getMovieVideos(@PathVariable("id") Long id) {
        return ResponseEntity.ok(
                tmdbService.getMovieVideos(id)
        );
    }

    /**
     * US18 – Movie details
     * GET /movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailsDto> getMovieDetails(@PathVariable("id") Long id) {
        return ResponseEntity.ok(
                tmdbService.getMovieDetails(id)
        );
    }
}
