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
    // FIXED: Recommendations by Genres (NOT comma-AND). We do per-genre discover + merge.
    // -------------------------------------------------------------------------
    public MovieSearchResponse getRecommendationsForUser(User user) {

        List<PreferenceScoreDto> genres = parse(user.getTopGenres());
        List<PreferenceScoreDto> actors = parse(user.getTopActors());
        List<PreferenceScoreDto> directors = parse(user.getTopDirectors());

        boolean hasData = !genres.isEmpty() || !actors.isEmpty() || !directors.isEmpty();
        if (!hasData) {
            return trendingFallback();
        }



        Map<Integer, MovieResultDto> unique = new LinkedHashMap<>();
        Random rnd = new Random();

        // 1) GENRES (per-genre discover, weighted by score order)
        // Action first (28) -> more chances to contribute results
        int maxGenreCalls = 5; // top N genres
        for (PreferenceScoreDto g : genres.stream().limit(maxGenreCalls).toList()) {
            if (g.getId() == null) continue;

            String genreIdStr = String.valueOf(g.getId());

            // 2 pages for top genre, 1 page for the rest (simple weighting)
            int pages = (g.equals(genres.get(0)) ? 2 : 1);

            for (int p = 0; p < pages; p++) {
                safeAddDiscover(unique,
                        1 + rnd.nextInt(2),           // page 1-2
                        "popularity.desc",
                        genreIdStr,                  // SINGLE GENRE ID (not CSV)
                        null,
                        null
                );
            }

            // stop early if we already have enough
            if (unique.size() >= 60) break;
        }

        // 2) ACTORS (works well already, keep it as-is)
        // Use only top few, but keep CSV for cast (it's okay)
        if (unique.size() < 60 && !actors.isEmpty()) {
            String withCastCsv = joinIds(actors, 3); // comma is fine here
            safeAddDiscover(unique, 1, "popularity.desc", null, withCastCsv, null);
        }

        // 3) DIRECTORS (works well already, keep it as-is)
        if (unique.size() < 60 && !directors.isEmpty()) {
            String withCrewCsv = joinIds(directors, 2);
            safeAddDiscover(unique, 1, "popularity.desc", null, null, withCrewCsv);
        }

        // 4) Fallback if still very few -> trending fill (NOT replace)
        if (unique.size() < 20) {
            MovieSearchResponse tr = trendingFallback();
            addResults(unique, tr);
        }

        // Filter out extremely “empty” items (optional but helps UI quality)
        List<MovieResultDto> results = unique.values().stream()
                .filter(m -> m != null && m.getTitle() != null && !m.getTitle().isBlank())
                .filter(m -> m.getPoster_path() != null && !m.getPoster_path().isBlank())
                .limit(60)
                .toList();

        MovieSearchResponse out = new MovieSearchResponse();
        out.setResults(results);

        if (out.getResults() == null || out.getResults().isEmpty()) {
            return trendingFallback();
        }

        return out;
    }

    private void safeAddDiscover(
            Map<Integer, MovieResultDto> unique,
            int page,
            String sortBy,
            String withGenresCsv,
            String withCastCsv,
            String withCrewCsv
    ) {
        boolean empty =
                (withGenresCsv == null || withGenresCsv.isBlank()) &&
                        (withCastCsv == null || withCastCsv.isBlank()) &&
                        (withCrewCsv == null || withCrewCsv.isBlank());

        if (empty) return;

        try {
            MovieSearchResponse r = tmdbService.discoverMovies(
                    page,
                    sortBy,
                    withGenresCsv,
                    withCastCsv,
                    withCrewCsv
            );
            addResults(unique, r);
        } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // QUIZ FEATURE (same as you had)
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
    // HELPERS
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

    private String joinIds(List<PreferenceScoreDto> list, int limit) {
        if (list == null || list.isEmpty()) return null;

        return list.stream()
                .map(PreferenceScoreDto::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .limit(Math.max(1, limit))
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
