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

    public MovieSearchResponse getRecommendationsForUser(User user) {

        List<PreferenceScoreDto> genres = parse(user.getTopGenres());
        List<PreferenceScoreDto> actors = parse(user.getTopActors());
        List<PreferenceScoreDto> directors = parse(user.getTopDirectors());

        boolean hasData =
                !genres.isEmpty() || !actors.isEmpty() || !directors.isEmpty();

        // FALLBACK 1 → TRENDING (NO PREFS)
        if (!hasData) {
            return trendingFallback();
        }

        // (κρατάμε το υπάρχον behavior για το /movies/recommendations)
        String withGenres = joinIds(genres);
        String withCast = joinIds(actors);
        String withCrew = joinIds(directors);

        MovieSearchResponse response = tmdbService.exploreMovies(
                1,
                "popularity.desc",
                null,
                null,
                null,
                withCast != null ? Long.valueOf(withCast.split(",")[0]) : null,
                withCrew != null ? Long.valueOf(withCrew.split(",")[0]) : null,
                withGenres != null ? Integer.valueOf(withGenres.split(",")[0]) : null
        );

        if (response.getResults() == null || response.getResults().isEmpty()) {
            return trendingFallback();
        }

        return response;
    }

    // ✅ QUIZ: "καθαρό" preference-based pool
    public List<MovieResultDto> getQuizCandidatesForUser(User user, int targetCount) {

        // ✅ Αν δεν υπάρχει user (guest) -> καθαρό trending pool
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

        // ✅ Αφαιρούμε primary_release_date.desc γιατί φέρνει “περίεργα”/upcoming vibes
        List<String> sorts = List.of(
                "popularity.desc",
                "vote_average.desc",
                "revenue.desc"
        );

        // unique pool
        Map<Integer, MovieResultDto> unique = new LinkedHashMap<>();

        // ✅ “Σφίγγουμε” τη μίξη: κυρίως genres, λιγότερο actors/directors
        int maxGenre = 6;
        int maxActor = 3;
        int maxDirector = 2;

        // --- 1) GENRES (κύριο σήμα)
        for (PreferenceScoreDto g : genres.stream().limit(maxGenre).toList()) {
            Integer genreId = g.getId() != null ? g.getId().intValue() : null;
            if (genreId == null) continue;

            for (int i = 0; i < 2; i++) { // 2 pages για variety
                MovieSearchResponse r = tmdbService.exploreMovies(
                        1 + rnd.nextInt(3),
                        sorts.get(rnd.nextInt(sorts.size())),
                        null, null,
                        5.5,          // ✅ minRating (κόβει πολύ “σκουπίδι”)
                        null, null,
                        genreId
                );
                addResults(unique, r);
            }
        }

        // --- 2) ACTORS (δευτερεύον σήμα)
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

        // --- 3) DIRECTORS (μικρό σήμα)
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

        // --- 4) Αν ακόμη λίγα: trending αλλά πάντα με φίλτρο
        if (unique.size() < Math.max(30, targetCount / 2)) {
            MovieSearchResponse tr = trendingFallback();
            addResults(unique, tr);
        }

        // --- 5) Τελικό: filter + shuffle + limit
        List<MovieResultDto> out = new ArrayList<>(unique.values());
        out.removeIf(m -> !isAcceptableForQuiz(m));

        Collections.shuffle(out, rnd);

        if (out.size() > targetCount) {
            return out.subList(0, targetCount);
        }
        return out;
    }

    // TRENDING → MovieSearchResponse
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

            // ✅ Βάζουμε στο pool, αλλά το τελικό φιλτράρισμα γίνεται στο τέλος
            unique.putIfAbsent(id, m);
        }
    }

    // ✅ quality gate για quiz candidates
    private boolean isAcceptableForQuiz(MovieResultDto m) {
        if (m == null) return false;
        if (m.getTitle() == null || m.getTitle().isBlank()) return false;

        // κόβουμε future years
        Integer y = extractYear(m.getRelease_date());
        if (y == null) return false;
        int maxYear = Year.now().getValue() - 1;
        if (y > maxYear) return false;

        // θέλουμε overview για “plot mentions” (και γενικά καλύτερη ποιότητα)
        if (m.getOverview() == null || m.getOverview().trim().length() < 60) return false;

        // αν έχει popularity, κράτα ένα ελάχιστο (αν είναι null, δεν κόβουμε)
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

    // HELPERS
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
