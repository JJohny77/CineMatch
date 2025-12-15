package com.cinematch.backend.service;

import com.cinematch.backend.dto.MovieResultDto;
import com.cinematch.backend.dto.MovieSearchResponse;
import com.cinematch.backend.dto.PreferenceScoreDto;
import com.cinematch.backend.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieRecommendationService {

    private final ObjectMapper objectMapper;
    private final TmdbService tmdbService;

    public MovieSearchResponse getRecommendationsForUser(User user) {

        List<PreferenceScoreDto> genres = parse(user.getTopGenres());
        List<PreferenceScoreDto> actors = parse(user.getTopActors());
        List<PreferenceScoreDto> directors = parse(user.getTopDirectors());

        boolean hasData =
                !genres.isEmpty() || !actors.isEmpty() || !directors.isEmpty();

        // =========================
        // FALLBACK 1 → TRENDING (NO PREFS)
        // =========================
        if (!hasData) {
            return trendingFallback();
        }

        // =========================
        // PERSONALIZED DISCOVER
        // =========================
        String withGenres = joinIds(genres);
        String withCast = joinIds(actors);
        String withCrew = joinIds(directors);

        MovieSearchResponse response = tmdbService.exploreMovies(
                1,                          // page
                "popularity.desc",          // sortBy
                null,                       // yearFrom
                null,                       // yearTo
                null,                       // minRating
                withCast != null ? Long.valueOf(withCast.split(",")[0]) : null,
                withCrew != null ? Long.valueOf(withCrew.split(",")[0]) : null,
                withGenres != null ? Integer.valueOf(withGenres.split(",")[0]) : null
        );

        // =========================
        // FALLBACK 2 → TRENDING (EMPTY DISCOVER)
        // =========================
        if (response.getResults() == null || response.getResults().isEmpty()) {
            return trendingFallback();
        }

        return response;
    }

    // =========================
    // TRENDING → MovieSearchResponse
    // =========================
    private MovieSearchResponse trendingFallback() {
        MovieSearchResponse response = new MovieSearchResponse();

        response.setResults(
                tmdbService.getTrendingMovies("day").stream()
                        .map(t -> {
                            MovieResultDto m = new MovieResultDto();
                            m.setId(t.getId().intValue());
                            m.setTitle(t.getTitle());
                            m.setOverview(t.getOverview());
                            m.setPoster_path(t.getPosterPath());
                            m.setRelease_date(t.getReleaseDate());
                            m.setPopularity(t.getPopularity());
                            return m;
                        })
                        .toList()
        );

        return response;
    }

    // =========================
    // HELPERS
    // =========================
    private String joinIds(List<PreferenceScoreDto> list) {
        return list.stream()
                .map(p -> String.valueOf(p.getId()))
                .limit(5)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    private List<PreferenceScoreDto> parse(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<PreferenceScoreDto>>() {}
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
