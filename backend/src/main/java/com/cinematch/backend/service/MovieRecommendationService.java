package com.cinematch.backend.service;

import com.cinematch.backend.dto.MovieResultDto;
import com.cinematch.backend.dto.MovieSearchResponse;
import com.cinematch.backend.dto.PreferenceScoreDto;
import com.cinematch.backend.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MovieRecommendationService {

    private final ObjectMapper objectMapper;
    private final TmdbService tmdbService;

    // -------------------------------------------------------------------------
    // MERGED LOGIC: Λογική αναζήτησης US58 (CSV) + Κώδικας Main (clean fallback)
    // -------------------------------------------------------------------------
    public MovieSearchResponse getRecommendationsForUser(User user) {

        List<PreferenceScoreDto> genres = parse(user.getTopGenres());
        List<PreferenceScoreDto> actors = parse(user.getTopActors());
        List<PreferenceScoreDto> directors = parse(user.getTopDirectors());

        boolean hasData = !genres.isEmpty() || !actors.isEmpty() || !directors.isEmpty();

        // Χρήση του helper από το Main (αντί για τον inline κώδικα του US58)
        if (!hasData) {
            return trendingFallback();
        }

        // Λογική από US58: Πολλαπλά IDs (CSV) και discoverMovies
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

        // Χρήση του helper από το Main για fallback
        if (discover == null || discover.getResults() == null || discover.getResults().isEmpty()) {
            return trendingFallback();
        }

        return discover;
    }

    // -------------------------------------------------------------------------
    // QUIZ FEATURE (Από το Main - Το US58 δεν το είχε καθόλου)
    // -------------------------------------------------------------------------
    public List<MovieResultDto> getQuizCandidatesForUser(User user, int targetCount) {

        if (user == null) {
            MovieSearchResponse tr = trendingFallback();
            List<MovieResultDto> out = new ArrayList<>(tr.getResults() == null ? List.of() : tr.getResults());
            out.removeIf(m -> !isAcceptableForQuiz(m));
            Collections.shuffle(out, new Random());
            return out.size() > targetCount ? out.subList(0, targetCount) : out;
        }

        List<PreferenceScoreDto> genres = parse(user.getTopGenres());
        List<PreferenceScoreDto> actors = parse(user.getTopActors());
        List<PreferenceScoreDto> directors = parse(user.getTopDirectors());

        boolean hasPrefs = !genres.isEmpty() || !actors.isEmpty() || !directors.isEmpty();
        if (!hasPrefs) {
            MovieSearchResponse tr = trendingFallback();
            List<MovieResultDto> out = new ArrayList<>(tr.getResults() == null ? List.of() : tr.getResults());
            out.removeIf(m -> !isAcceptableForQuiz(m));
            Collections.shuffle(out, new Random());
            return out.size() > targetCount ? out.subList(0, targetCount) : out;
        }

        Random rnd = new Random();
        List<String> sorts = List.of(
                "popularity.desc",
                "vote_average.desc",
                "revenue.desc"
        );

        Map<Integer, MovieResultDto> unique = new LinkedHashMap<>();

        int maxGenre = 6;
        int maxActor = 3;
        int maxDirector = 2;

        // 1) GENRES
        for (PreferenceScoreDto g : genres.stream().limit(maxGenre).toList()) {
            Integer genreId = g.getId() != null ? g.getId().intValue() : null;
            if (genreId == null) continue;

            for (int i = 0; i < 2; i++) {
                MovieSearchResponse r = tmdbService.exploreMovies(
                        1 + rnd.nextInt(3),
                        sorts.get(rnd.nextInt(sorts.size())),
                        null, null,
                        5.5,
                        null, null,
                        genreId
                );
                addResults(unique, r);
            }
        }

        // 2) ACTORS
        for (PreferenceScoreDto a : actors.stream().limit(maxActor).toList()) {
            Long castId = a.getId();
            if (castId == null) continue;

            MovieSearchResponse r = tmdbService.exploreMovies(
                    1 + rnd.nextInt(3),
                    "popularity.desc",
                    null, null,
                    5.5,
                    castId,
                    null,
                    null
            );
            addResults(unique, r);
        }

        // 3) DIRECTORS
        for (PreferenceScoreDto d : directors.stream().limit(maxDirector).toList()) {
            Long crewId = d.getId();
            if (crewId == null) continue;

            MovieSearchResponse r = tmdbService.exploreMovies(
                    1 + rnd.nextInt(3),
                    "popularity.desc",
                    null, null,
                    5.5,
                    null,
                    crewId,
                    null
            );
            addResults(unique, r);
        }

        // 4) Fill with trending if low
        if (unique.size() < Math.max(30, targetCount / 2)) {
            MovieSearchResponse tr = trendingFallback();
            addResults(unique, tr);
        }

        // 5) Final filter
        List<MovieResultDto> out = new ArrayList<>(unique.values());
        out.removeIf(m -> !isAcceptableForQuiz(m));

        Collections.shuffle(out, rnd);

        if (out.size() > targetCount) {
            return out.subList(0, targetCount);
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // HELPERS (Από το Main - Απαραίτητα για Quiz & Fallback)
    // -------------------------------------------------------------------------

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

    private void addResults(Map<Integer, MovieResultDto> unique, MovieSearchResponse response) {
        if (response == null || response.getResults() == null) return;
        for (MovieResultDto m : response.getResults()) {
            if (m == null) continue;
            int id = m.getId();
            if (id <= 0) continue;
            unique.putIfAbsent(id, m);
        }
    }

    private boolean isAcceptableForQuiz(MovieResultDto m) {
        if (m == null) return false;
        if (m.getTitle() == null || m.getTitle().isBlank()) return false;

        Integer y = extractYear(m.getRelease_date());
        if (y == null) return false;
        int maxYear = Year.now().getValue() - 1;
        if (y > maxYear) return false;

        if (m.getOverview() == null || m.getOverview().trim().length() < 60) return false;

        try {
            Double pop = m.getPopularity();
            if (pop != null && pop < 5.0) return false;
        } catch (Exception ignored) {}

        return true;
    }

    private Integer extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try { return Integer.parseInt(releaseDate.substring(0, 4)); }
        catch (Exception e) { return null; }
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