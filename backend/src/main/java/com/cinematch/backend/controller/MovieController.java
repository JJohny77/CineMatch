package com.cinematch.backend.controller;

import com.cinematch.backend.dto.*;
import com.cinematch.backend.service.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * US45 – Explore Movies
     * GET /movies/explore
     *
     * Filters:
     * - page
     * - sortBy
     * - yearFrom / yearTo
     * - minRating
     * - castId (actor)
     * - crewId (director)
     * - genreId
     */
    @GetMapping("/explore")
    public MovieSearchResponse exploreMovies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "popularity.desc") String sortBy,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Long castId,
            @RequestParam(required = false) Long crewId,
            @RequestParam(required = false) Integer genreId
    ) {
        return tmdbService.exploreMovies(
                page,
                sortBy,
                yearFrom,
                yearTo,
                minRating,
                castId,
                crewId,
                genreId
        );
    }

    /**
     * Genres
     * GET /movies/genres
     */
    @GetMapping("/genres")
    public GenreListResponse getGenres() {
        return tmdbService.getMovieGenres();
    }

    /**
     * Search persons (actors/directors) by name.
     * GET /movies/person/search?query=tom
     */
    @GetMapping("/person/search")
    public PersonSearchResponse searchPerson(@RequestParam String query) {
        return tmdbService.searchPerson(query);
    }

    /**
     * US18 – Movie Videos (trailers)
     * GET /movies/{id}/videos
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
