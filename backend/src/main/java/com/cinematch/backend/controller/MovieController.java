package com.cinematch.backend.controller;

import com.cinematch.backend.dto.*;
import com.cinematch.backend.model.User;
import com.cinematch.backend.model.UserEventType;
import com.cinematch.backend.service.CurrentUserService;
import com.cinematch.backend.service.TmdbService;
import com.cinematch.backend.service.UserEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final TmdbService tmdbService;
    private final CurrentUserService currentUserService;
    private final UserEventService userEventService;

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
     * Trending actors
     * GET /movies/trending-actors?time_window=day
     */
    @GetMapping("/trending-actors")
    public ResponseEntity<List<TrendingPersonDto>> getTrendingActors(
            @RequestParam(defaultValue = "day") String time_window
    ) {
        return ResponseEntity.ok(
                tmdbService.getTrendingActors(time_window)
        );
    }

    /**
     * Trending directors
     * GET /movies/trending-directors?time_window=day
     */
    @GetMapping("/trending-directors")
    public ResponseEntity<List<TrendingPersonDto>> getTrendingDirectors(
            @RequestParam(defaultValue = "day") String time_window
    ) {
        return ResponseEntity.ok(
                tmdbService.getTrendingDirectors(time_window)
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
        MovieSearchResponse response = tmdbService.exploreMovies(
                page,
                sortBy,
                yearFrom,
                yearTo,
                minRating,
                castId,
                crewId,
                genreId
        );

        // ==========================
        // CHOOSE_FILTER event
        // ==========================
        User user = currentUserService.getCurrentUserOrNull();

        boolean hasAnyFilter =
                yearFrom != null ||
                        yearTo != null ||
                        minRating != null ||
                        castId != null ||
                        crewId != null ||
                        genreId != null;

        if (user != null && hasAnyFilter) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("context", "MOVIE_EXPLORE");
            payload.put("page", page);
            payload.put("sortBy", sortBy);
            if (yearFrom != null) payload.put("yearFrom", yearFrom);
            if (yearTo != null) payload.put("yearTo", yearTo);
            if (minRating != null) payload.put("minRating", minRating);
            if (castId != null) payload.put("castId", castId);
            if (crewId != null) payload.put("crewId", crewId);
            if (genreId != null) payload.put("genreId", genreId);

            userEventService.logEvent(
                    user,
                    UserEventType.CHOOSE_FILTER,
                    payload
            );
        }

        return response;
    }

    /**
     * Genres
     * GET /movies/genres
     */
    @GetMapping("/genres")
    public GenreListResponse getGenres() {
        return tmdbService.getMovieGenres();
    }

    @GetMapping("/person/search")
    public ResponseEntity<PersonSearchResponseDto> searchPerson(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page
    ) {
        if (query.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                tmdbService.searchPerson(query, page)
        );
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
     *
     * Optional query param "source" για να ξέρουμε από πού άνοιξε η ταινία
     * (π.χ. "SEARCH", "HOME", "TRENDING", "EXPLORE" κ.λπ.).
     */
    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailsDto> getMovieDetails(
            @PathVariable("id") Long id,
            @RequestParam(name = "source", required = false) String source
    ) {
        MovieDetailsDto dto = tmdbService.getMovieDetails(id);

        User user = currentUserService.getCurrentUserOrNull();
        if (user != null) {
            String actualSource =
                    (source != null && !source.isBlank()) ? source : "MOVIE_DETAILS";

            Map<String, Object> payload = new HashMap<>();
            payload.put("movieId", id);
            payload.put("source", actualSource);

            userEventService.logEvent(
                    user,
                    UserEventType.OPEN_MOVIE,
                    payload
            );
        }

        return ResponseEntity.ok(dto);
    }
}
