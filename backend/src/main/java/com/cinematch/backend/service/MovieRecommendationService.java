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

        boolean hasData = !genres.isEmpty() || !actors.isEmpty() || !directors.isEmpty();

        // =========================
        // FALLBACK → TRENDING
        // =========================
        if (!hasData) {
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
        // PERSONALIZED DISCOVER
        // (δώσε ΠΟΛΛΑ ids σε CSV)
        // =========================
        String withGenresCsv = joinIds(genres);
        String withCastCsv = joinIds(actors);
        String withCrewCsv = joinIds(directors);

        MovieSearchResponse discover = tmdbService.discoverMovies(
                1,
                "popularity.desc",
                withGenresCsv,
                withCastCsv,
                withCrewCsv
        );

        // αν για κάποιο λόγο βγει άδειο -> fallback trending
        if (discover == null || discover.getResults() == null || discover.getResults().isEmpty()) {
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

        return discover;
    }

    private String joinIds(List<PreferenceScoreDto> list) {
        if (list == null || list.isEmpty()) return null;

        return list.stream()
                .map(p -> String.valueOf(p.getId()))
                .limit(5)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    private List<PreferenceScoreDto> parse(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PreferenceScoreDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
