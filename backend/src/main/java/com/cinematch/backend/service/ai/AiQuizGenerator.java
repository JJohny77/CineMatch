package com.cinematch.backend.service.ai;

import com.cinematch.backend.dto.MovieResultDto;
import com.cinematch.backend.dto.PreferenceScoreDto;
import com.cinematch.backend.dto.UserPreferencesResponseDto;
import com.cinematch.backend.quiz.service.FullQuestion;
import com.cinematch.backend.service.TmdbService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Year;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuizGenerator {

    private final ObjectMapper objectMapper;
    private final TmdbService tmdbService;

    @Value("${huggingface.api.key:}")
    private String apiKey;

    // Optional (αν δεν το βάλεις στο application.properties, έχει default)
    @Value("${huggingface.quiz.model-url:https://router.huggingface.co/hf-inference/models/mistralai/Mistral-7B-Instruct-v0.2}")
    private String modelUrl;

    // =========================
    // In-memory cache για TMDb movie meta (avoid πολλά calls)
    // =========================
    private final Map<Long, MovieSeed> seedCache = new ConcurrentHashMap<>();

    private RestTemplate restTemplateWithTimeouts() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(7000);
        f.setReadTimeout(20000);
        return new RestTemplate(f);
    }

    // ==========================================================
    // MAIN entry
    // ==========================================================
    public List<FullQuestion> generateFullQuestions(
            UserPreferencesResponseDto prefs,
            List<MovieResultDto> movies,
            int count
    ) {
        int maxYear = Year.now().getValue() - 1; // κόβουμε upcoming
        if (movies == null) movies = Collections.emptyList();
        if (count <= 0) return Collections.emptyList();

        // 1) Φτιάχνουμε “seeds” από candidates (μόνο όσα χρειάζονται)
        List<MovieSeed> seeds = buildEnrichedSeeds(movies, 42, maxYear);
        if (seeds.size() < 8) return Collections.emptyList();

        // 2) Δημιουργούμε deterministic questions (σωστά 100%)
        List<FullQuestion> base = buildDeterministicQuestions(prefs, seeds, count, maxYear);

        // 3) Προαιρετικά: HuggingFace μόνο για paraphrase (όχι για logic)
        //    - Δεν αλλάζουμε correct answers, μόνο το questionText.
        if (apiKey != null && !apiKey.isBlank() && base != null && base.size() >= Math.min(6, count)) {
            try {
                List<String> rewritten = paraphraseQuestions(base.stream().map(FullQuestion::question).toList());
                if (rewritten != null && rewritten.size() == base.size()) {
                    List<FullQuestion> out = new ArrayList<>();
                    for (int i = 0; i < base.size(); i++) {
                        FullQuestion fq = base.get(i);
                        String q2 = rewritten.get(i);
                        if (q2 != null && !q2.isBlank()) {
                            out.add(new FullQuestion(q2.trim(), fq.correctAnswer(), fq.options()));
                        } else {
                            out.add(fq);
                        }
                    }
                    return out;
                }
            } catch (Exception ex) {
                log.warn("HF paraphrase failed -> keep templates. {}", ex.getMessage());
            }
        }

        return base;
    }

    // ==========================================================
    // Step 1: enrich seeds with TMDb genres + credits
    // ==========================================================
    private List<MovieSeed> buildEnrichedSeeds(List<MovieResultDto> movies, int maxSeeds, int maxYear) {
        List<MovieResultDto> usable = movies.stream()
                .filter(m -> m != null && m.getTitle() != null && !m.getTitle().isBlank())
                .filter(m -> {
                    Integer y = extractYear(m.getRelease_date());
                    return y != null && y <= maxYear;
                })
                .toList();

        if (usable.isEmpty()) return List.of();

        List<MovieResultDto> shuffled = new ArrayList<>(usable);
        Collections.shuffle(shuffled, new Random());

        List<MovieSeed> out = new ArrayList<>();

        for (MovieResultDto m : shuffled) {
            if (out.size() >= maxSeeds) break;

            long id = m.getId();
            if (id <= 0) continue;

            MovieSeed cached = seedCache.get(id);
            if (cached != null) {
                out.add(cached);
                continue;
            }

            try {
                MovieSeed seed = fetchMovieSeedFromTmdb(id, m);
                if (seed == null) continue;

                // quality gate: θέλουμε overview για keyword-type
                if (seed.overview == null || seed.overview.trim().length() < 60) continue;

                seedCache.put(id, seed);
                out.add(seed);

            } catch (Exception ignored) {
                // ignore one movie; keep building
            }
        }

        return out;
    }

    private MovieSeed fetchMovieSeedFromTmdb(long movieId, MovieResultDto fallback) {
        Map<String, String> params = new HashMap<>();
        params.put("language", "en-US");
        params.put("append_to_response", "credits");

        String json = tmdbService.fetchFromTmdb("/movie/" + movieId, params);

        try {
            JsonNode root = objectMapper.readTree(json);

            String title = text(root, "title");
            if (title == null || title.isBlank()) title = fallback.getTitle();

            String overview = text(root, "overview");
            if (overview == null || overview.isBlank()) overview = fallback.getOverview();

            String releaseDate = text(root, "release_date");

            Integer year = extractYear(releaseDate);
            if (year == null) year = extractYear(fallback.getRelease_date());

            // popularity: MovieResultDto.popularity είναι primitive double (never null)
            double popularity = root.hasNonNull("popularity")
                    ? root.get("popularity").asDouble()
                    : fallback.getPopularity();

            // runtime (minutes) - optional
            Integer runtime = null;
            if (root.hasNonNull("runtime") && root.get("runtime").isNumber()) {
                int rt = root.get("runtime").asInt();
                if (rt > 0) runtime = rt;
            }

            // genres
            Set<Integer> genreIds = new HashSet<>();
            Map<Integer, String> genreNames = new HashMap<>();
            if (root.has("genres") && root.get("genres").isArray()) {
                for (JsonNode g : root.get("genres")) {
                    if (g != null && g.hasNonNull("id")) {
                        int gid = g.get("id").asInt();
                        genreIds.add(gid);
                        String gname = text(g, "name");
                        if (gname != null && !gname.isBlank()) genreNames.put(gid, gname);
                    }
                }
            }

            // credits
            Set<Long> castIds = new HashSet<>();
            Map<Long, String> castNames = new HashMap<>();
            Long directorId = null;
            String directorName = null;

            JsonNode credits = root.get("credits");
            if (credits != null && credits.isObject()) {

                // cast (top 12)
                JsonNode cast = credits.get("cast");
                if (cast != null && cast.isArray()) {
                    int limit = Math.min(12, cast.size());
                    for (int i = 0; i < limit; i++) {
                        JsonNode c = cast.get(i);
                        if (c == null || !c.hasNonNull("id")) continue;
                        long pid = c.get("id").asLong();
                        String name = text(c, "name");
                        if (name == null || name.isBlank()) continue;
                        castIds.add(pid);
                        castNames.put(pid, name);
                    }
                }

                // director from crew
                JsonNode crew = credits.get("crew");
                if (crew != null && crew.isArray()) {
                    for (JsonNode cr : crew) {
                        if (cr == null) continue;
                        String job = text(cr, "job");
                        if (job != null && job.equalsIgnoreCase("Director")) {
                            if (cr.hasNonNull("id")) directorId = cr.get("id").asLong();
                            directorName = text(cr, "name");
                            break;
                        }
                    }
                }
            }

            MovieSeed seed = new MovieSeed();
            seed.id = movieId;
            seed.title = title;
            seed.overview = overview;
            seed.year = year;
            seed.popularity = popularity;
            seed.runtime = runtime;

            seed.genreIds = genreIds;
            seed.genreNames = genreNames;
            seed.castIds = castIds;
            seed.castNames = castNames;
            seed.directorId = directorId;
            seed.directorName = directorName;

            return seed;

        } catch (Exception e) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v != null && v.isTextual()) ? v.asText() : null;
    }

    // ==========================================================
    // Step 2: Deterministic generation (preference-based)
    // ==========================================================
    private List<FullQuestion> buildDeterministicQuestions(
            UserPreferencesResponseDto prefs,
            List<MovieSeed> seeds,
            int count,
            int maxYear
    ) {
        Random rnd = new Random();

        List<Long> prefActors = (prefs == null) ? List.of() : idsLong(prefs.getTopActors(), 10);
        List<Long> prefDirectors = (prefs == null) ? List.of() : idsLong(prefs.getTopDirectors(), 8);
        List<Integer> prefGenres = (prefs == null) ? List.of() : idsInt(prefs.getTopGenres(), 10);

        int prefTarget = (hasPrefs(prefActors, prefDirectors, prefGenres))
                ? Math.max(6, (int) Math.ceil(count * 0.6))
                : 0;

        List<QuestionType> plan = new ArrayList<>();

        // preference-heavy πρώτα (όσο έχουμε)
        if (!prefGenres.isEmpty()) addRepeated(plan, QuestionType.GENRE, Math.min(3, prefTarget));
        if (!prefActors.isEmpty()) addRepeated(plan, QuestionType.ACTOR, Math.min(3, Math.max(0, prefTarget - plan.size())));
        if (!prefDirectors.isEmpty()) addRepeated(plan, QuestionType.DIRECTOR, Math.min(2, Math.max(0, prefTarget - plan.size())));

        // extra genre variety (αν έχουμε genres)
        if (!prefGenres.isEmpty() && plan.size() < prefTarget) {
            plan.add(QuestionType.GENRE_NEG);
        }

        while (plan.size() < count) {
            plan.add(pickDiversifier(plan));
        }

        Collections.shuffle(plan, rnd);

        List<FullQuestion> out = new ArrayList<>();
        Set<String> usedQuestions = new HashSet<>();
        Set<String> usedCorrectTitles = new HashSet<>();
        Set<String> usedEntities = new HashSet<>(); // "A:123", "G:28", "D:525", "Y:2016", "K:word", ...

        int attempts = 0;

        while (out.size() < count && attempts < 1200) {
            attempts++;

            QuestionType type = plan.get(out.size());

            FullQuestion fq = switch (type) {
                case ACTOR -> buildActorQuestion(seeds, prefActors, usedEntities, usedCorrectTitles, maxYear);
                case DIRECTOR -> buildDirectorQuestion(seeds, prefDirectors, usedEntities, usedCorrectTitles, maxYear);
                case GENRE -> buildGenreQuestion(seeds, prefGenres, usedEntities, usedCorrectTitles, maxYear);
                case GENRE_NEG -> buildGenreNegQuestion(seeds, prefGenres, usedEntities, usedCorrectTitles, maxYear);

                case YEAR -> buildYearQuestion(seeds, usedEntities, usedCorrectTitles, maxYear);
                case NEWEST -> buildNewestQuestion(seeds, usedEntities, usedCorrectTitles, maxYear);
                case OLDEST -> buildOldestQuestion(seeds, usedEntities, usedCorrectTitles, maxYear);

                case POPULARITY_MAX -> buildPopularityMaxQuestion(seeds, usedEntities, usedCorrectTitles, maxYear);
                case POPULARITY_MIN -> buildPopularityMinQuestion(seeds, usedEntities, usedCorrectTitles, maxYear);

                case RUNTIME_MAX -> buildRuntimeQuestion(seeds, usedEntities, usedCorrectTitles, maxYear, true);
                case RUNTIME_MIN -> buildRuntimeQuestion(seeds, usedEntities, usedCorrectTitles, maxYear, false);

                case KEYWORD -> buildKeywordQuestion(seeds, usedEntities, usedCorrectTitles, maxYear);
            };

            if (fq == null) continue;

            String qKey = fq.question().trim().toLowerCase(Locale.ROOT);
            if (!usedQuestions.add(qKey)) continue;

            if (fq.options() == null || fq.options().size() != 4) continue;
            if (new HashSet<>(fq.options()).size() != 4) continue;

            out.add(fq);
        }

        // last resort fill
        while (out.size() < count) {
            FullQuestion fq = buildPopularityMaxQuestion(seeds, usedEntities, usedCorrectTitles, maxYear);
            if (fq == null) fq = buildYearQuestion(seeds, usedEntities, usedCorrectTitles, maxYear);
            if (fq == null) fq = buildKeywordQuestion(seeds, usedEntities, usedCorrectTitles, maxYear);
            if (fq == null) break;
            out.add(fq);
        }

        return out;
    }

    private boolean hasPrefs(List<Long> a, List<Long> d, List<Integer> g) {
        return (a != null && !a.isEmpty()) || (d != null && !d.isEmpty()) || (g != null && !g.isEmpty());
    }

    private void addRepeated(List<QuestionType> plan, QuestionType t, int times) {
        for (int i = 0; i < times; i++) plan.add(t);
    }

    private QuestionType pickDiversifier(List<QuestionType> plan) {
        // θέλουμε να σπάμε τα “year/keyword” μοτίβα
        List<QuestionType> all = List.of(
                QuestionType.YEAR,
                QuestionType.NEWEST,
                QuestionType.OLDEST,
                QuestionType.KEYWORD,
                QuestionType.POPULARITY_MAX,
                QuestionType.POPULARITY_MIN,
                QuestionType.RUNTIME_MAX
        );

        QuestionType best = all.get(0);
        int bestCount = Integer.MAX_VALUE;

        for (QuestionType t : all) {
            int c = count(plan, t);
            if (c < bestCount) {
                best = t;
                bestCount = c;
            }
        }

        return best;
    }

    private int count(List<QuestionType> list, QuestionType t) {
        int c = 0;
        for (QuestionType x : list) if (x == t) c++;
        return c;
    }

    // =========================
    // ACTOR
    // =========================
    private FullQuestion buildActorQuestion(
            List<MovieSeed> seeds,
            List<Long> prefActors,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        if (prefActors == null || prefActors.isEmpty()) return null;

        Random rnd = new Random();
        List<Long> shuffled = new ArrayList<>(prefActors);
        Collections.shuffle(shuffled, rnd);

        for (Long actorId : shuffled) {
            if (actorId == null) continue;
            String entityKey = "A:" + actorId;
            if (usedEntities.contains(entityKey)) continue;

            List<MovieSeed> with = seeds.stream()
                    .filter(s -> s != null && s.year != null && s.year <= maxYear)
                    .filter(s -> s.castIds != null && s.castIds.contains(actorId))
                    .toList();

            if (with.isEmpty()) continue;

            MovieSeed correct = with.get(rnd.nextInt(with.size()));
            if (correct.title == null) continue;
            if (!usedCorrectTitles.add(correct.title.toLowerCase(Locale.ROOT))) continue;

            String actorName = (correct.castNames != null) ? correct.castNames.get(actorId) : null;
            if (actorName == null || actorName.isBlank()) actorName = "this actor";

            List<MovieSeed> without = new ArrayList<>(seeds.stream()
                    .filter(s -> s != null && s.title != null)
                    .filter(s -> s.castIds == null || !s.castIds.contains(actorId))
                    .filter(s -> !s.title.equalsIgnoreCase(correct.title))
                    .toList());

            if (without.size() < 3) continue;

            Collections.shuffle(without, rnd);
            List<MovieSeed> d3 = without.subList(0, 3);

            List<String> options = new ArrayList<>();
            options.add(correct.title);
            options.add(d3.get(0).title);
            options.add(d3.get(1).title);
            options.add(d3.get(2).title);

            if (new HashSet<>(options).size() != 4) continue;

            usedEntities.add(entityKey);

            String q = "Which of the following movies features " + actorName + "?";
            return new FullQuestion(q, correct.title, options);
        }

        return null;
    }

    // =========================
    // DIRECTOR
    // =========================
    private FullQuestion buildDirectorQuestion(
            List<MovieSeed> seeds,
            List<Long> prefDirectors,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        if (prefDirectors == null || prefDirectors.isEmpty()) return null;

        Random rnd = new Random();
        List<Long> shuffled = new ArrayList<>(prefDirectors);
        Collections.shuffle(shuffled, rnd);

        for (Long directorId : shuffled) {
            if (directorId == null) continue;
            String entityKey = "D:" + directorId;
            if (usedEntities.contains(entityKey)) continue;

            List<MovieSeed> with = seeds.stream()
                    .filter(s -> s != null && s.year != null && s.year <= maxYear)
                    .filter(s -> s.directorId != null && s.directorId.equals(directorId))
                    .toList();

            if (with.isEmpty()) continue;

            MovieSeed correct = with.get(rnd.nextInt(with.size()));
            if (correct.title == null) continue;
            if (!usedCorrectTitles.add(correct.title.toLowerCase(Locale.ROOT))) continue;

            String directorName = (correct.directorName != null && !correct.directorName.isBlank())
                    ? correct.directorName
                    : "this director";

            List<MovieSeed> without = new ArrayList<>(seeds.stream()
                    .filter(s -> s != null && s.title != null)
                    .filter(s -> s.directorId == null || !s.directorId.equals(directorId))
                    .filter(s -> !s.title.equalsIgnoreCase(correct.title))
                    .toList());

            if (without.size() < 3) continue;

            Collections.shuffle(without, rnd);
            List<MovieSeed> d3 = without.subList(0, 3);

            List<String> options = new ArrayList<>();
            options.add(correct.title);
            options.add(d3.get(0).title);
            options.add(d3.get(1).title);
            options.add(d3.get(2).title);

            if (new HashSet<>(options).size() != 4) continue;

            usedEntities.add(entityKey);

            String q = "Which of the following movies was directed by " + directorName + "?";
            return new FullQuestion(q, correct.title, options);
        }

        return null;
    }

    // =========================
    // GENRE (positive)
    // =========================
    private FullQuestion buildGenreQuestion(
            List<MovieSeed> seeds,
            List<Integer> prefGenres,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        if (prefGenres == null || prefGenres.isEmpty()) return null;

        Random rnd = new Random();
        List<Integer> shuffled = new ArrayList<>(prefGenres);
        Collections.shuffle(shuffled, rnd);

        for (Integer genreId : shuffled) {
            if (genreId == null) continue;
            String entityKey = "G:" + genreId;
            if (usedEntities.contains(entityKey)) continue;

            List<MovieSeed> with = seeds.stream()
                    .filter(s -> s != null && s.year != null && s.year <= maxYear)
                    .filter(s -> s.genreIds != null && s.genreIds.contains(genreId))
                    .toList();

            if (with.isEmpty()) continue;

            MovieSeed correct = with.get(rnd.nextInt(with.size()));
            if (correct.title == null) continue;
            if (!usedCorrectTitles.add(correct.title.toLowerCase(Locale.ROOT))) continue;

            String genreName = (correct.genreNames != null) ? correct.genreNames.get(genreId) : null;
            if (genreName == null || genreName.isBlank()) genreName = "this genre";

            List<MovieSeed> without = new ArrayList<>(seeds.stream()
                    .filter(s -> s != null && s.title != null)
                    .filter(s -> s.genreIds == null || !s.genreIds.contains(genreId))
                    .filter(s -> !s.title.equalsIgnoreCase(correct.title))
                    .toList());

            if (without.size() < 3) continue;

            Collections.shuffle(without, rnd);
            List<MovieSeed> d3 = without.subList(0, 3);

            List<String> options = new ArrayList<>();
            options.add(correct.title);
            options.add(d3.get(0).title);
            options.add(d3.get(1).title);
            options.add(d3.get(2).title);

            if (new HashSet<>(options).size() != 4) continue;

            usedEntities.add(entityKey);

            String q = "Which of the following movies belongs to the " + genreName + " genre?";
            return new FullQuestion(q, correct.title, options);
        }

        return null;
    }

    // =========================
    // GENRE (negative) - 3 are genre, 1 is NOT genre
    // =========================
    private FullQuestion buildGenreNegQuestion(
            List<MovieSeed> seeds,
            List<Integer> prefGenres,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        if (prefGenres == null || prefGenres.isEmpty()) return null;

        Random rnd = new Random();
        List<Integer> shuffled = new ArrayList<>(prefGenres);
        Collections.shuffle(shuffled, rnd);

        for (Integer genreId : shuffled) {
            if (genreId == null) continue;
            String entityKey = "GN:" + genreId;
            if (usedEntities.contains(entityKey)) continue;

            List<MovieSeed> with = seeds.stream()
                    .filter(s -> s != null && s.title != null && s.year != null && s.year <= maxYear)
                    .filter(s -> s.genreIds != null && s.genreIds.contains(genreId))
                    .toList();

            List<MovieSeed> without = seeds.stream()
                    .filter(s -> s != null && s.title != null && s.year != null && s.year <= maxYear)
                    .filter(s -> s.genreIds == null || !s.genreIds.contains(genreId))
                    .toList();

            if (with.size() < 3 || without.isEmpty()) continue;

            MovieSeed correct = without.get(rnd.nextInt(without.size())); // NOT genre
            if (!usedCorrectTitles.add(correct.title.toLowerCase(Locale.ROOT))) continue;

            String genreName = null;
            for (MovieSeed any : with) {
                if (any.genreNames != null && any.genreNames.get(genreId) != null) {
                    genreName = any.genreNames.get(genreId);
                    break;
                }
            }
            if (genreName == null || genreName.isBlank()) genreName = "this genre";

            List<MovieSeed> w = new ArrayList<>(with);
            Collections.shuffle(w, rnd);

            List<String> options = new ArrayList<>();
            options.add(correct.title);
            options.add(w.get(0).title);
            options.add(w.get(1).title);
            options.add(w.get(2).title);

            if (new HashSet<>(options).size() != 4) continue;

            usedEntities.add(entityKey);

            String q = "Which of the following movies does NOT belong to the " + genreName + " genre?";
            return new FullQuestion(q, correct.title, options);
        }

        return null;
    }

    // =========================
    // YEAR
    // =========================
    private FullQuestion buildYearQuestion(
            List<MovieSeed> seeds,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        Random rnd = new Random();

        List<MovieSeed> pool = new ArrayList<>(seeds.stream()
                .filter(s -> s != null && s.title != null && s.year != null && s.year <= maxYear)
                .toList());

        if (pool.size() < 4) return null;

        Collections.shuffle(pool, rnd);
        MovieSeed correct = pool.get(0);
        if (!usedCorrectTitles.add(correct.title.toLowerCase(Locale.ROOT))) return null;

        int year = correct.year;
        String entityKey = "Y:" + year;
        if (!usedEntities.add(entityKey)) return null;

        List<MovieSeed> distractors = new ArrayList<>(pool.stream()
                .filter(s -> !s.title.equalsIgnoreCase(correct.title))
                .toList());

        if (distractors.size() < 3) return null;

        Collections.shuffle(distractors, rnd);
        List<MovieSeed> d3 = distractors.subList(0, 3);

        List<String> options = new ArrayList<>();
        options.add(correct.title);
        options.add(d3.get(0).title);
        options.add(d3.get(1).title);
        options.add(d3.get(2).title);

        if (new HashSet<>(options).size() != 4) return null;

        String q = "Which of the following movies was released in " + year + "?";
        return new FullQuestion(q, correct.title, options);
    }

    // =========================
    // NEWEST / OLDEST (based on year among 4)
    // =========================
    private FullQuestion buildNewestQuestion(
            List<MovieSeed> seeds,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        return buildYearExtremeQuestion(seeds, usedEntities, usedCorrectTitles, maxYear, true);
    }

    private FullQuestion buildOldestQuestion(
            List<MovieSeed> seeds,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        return buildYearExtremeQuestion(seeds, usedEntities, usedCorrectTitles, maxYear, false);
    }

    private FullQuestion buildYearExtremeQuestion(
            List<MovieSeed> seeds,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear,
            boolean newest
    ) {
        Random rnd = new Random();

        List<MovieSeed> pool = new ArrayList<>(seeds.stream()
                .filter(s -> s != null && s.title != null && s.year != null && s.year <= maxYear)
                .toList());

        if (pool.size() < 4) return null;

        Collections.shuffle(pool, rnd);
        List<MovieSeed> pick4 = pool.subList(0, 4);

        MovieSeed correct = newest
                ? pick4.stream().max(Comparator.comparingInt(s -> s.year)).orElse(null)
                : pick4.stream().min(Comparator.comparingInt(s -> s.year)).orElse(null);

        if (correct == null) return null;
        if (!usedCorrectTitles.add(correct.title.toLowerCase(Locale.ROOT))) return null;

        String entityKey = (newest ? "NEW:" : "OLD:") + correct.id;
        if (!usedEntities.add(entityKey)) return null;

        List<String> options = new ArrayList<>(pick4.stream().map(s -> s.title).toList());
        if (new HashSet<>(options).size() != 4) return null;

        String q = newest
                ? "Which of the following movies is the newest (most recent release year)?"
                : "Which of the following movies is the oldest (earliest release year)?";

        return new FullQuestion(q, correct.title, options);
    }

    // =========================
    // POPULARITY (max/min among options)
    // =========================
    private FullQuestion buildPopularityMaxQuestion(
            List<MovieSeed> seeds,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        return buildPopularityExtremeQuestion(seeds, usedEntities, usedCorrectTitles, maxYear, true);
    }

    private FullQuestion buildPopularityMinQuestion(
            List<MovieSeed> seeds,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        return buildPopularityExtremeQuestion(seeds, usedEntities, usedCorrectTitles, maxYear, false);
    }

    private FullQuestion buildPopularityExtremeQuestion(
            List<MovieSeed> seeds,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear,
            boolean max
    ) {
        Random rnd = new Random();

        List<MovieSeed> pool = new ArrayList<>(seeds.stream()
                .filter(s -> s != null && s.title != null && s.year != null && s.year <= maxYear)
                .toList());

        if (pool.size() < 4) return null;

        Collections.shuffle(pool, rnd);
        List<MovieSeed> pick4 = pool.subList(0, 4);

        MovieSeed correct = max
                ? pick4.stream().max(Comparator.comparingDouble(s -> s.popularity)).orElse(null)
                : pick4.stream().min(Comparator.comparingDouble(s -> s.popularity)).orElse(null);

        if (correct == null) return null;
        if (!usedCorrectTitles.add(correct.title.toLowerCase(Locale.ROOT))) return null;

        String entityKey = (max ? "P_MAX:" : "P_MIN:") + correct.id;
        if (!usedEntities.add(entityKey)) return null;

        List<String> options = new ArrayList<>(pick4.stream().map(s -> s.title).toList());
        if (new HashSet<>(options).size() != 4) return null;

        String q = max
                ? "Which of the following movies is the most popular?"
                : "Which of the following movies is the least popular?";

        return new FullQuestion(q, correct.title, options);
    }

    // =========================
    // RUNTIME (longest/shortest among 4)
    // =========================
    private FullQuestion buildRuntimeQuestion(
            List<MovieSeed> seeds,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear,
            boolean longest
    ) {
        Random rnd = new Random();

        List<MovieSeed> pool = new ArrayList<>(seeds.stream()
                .filter(s -> s != null && s.title != null && s.year != null && s.year <= maxYear)
                .filter(s -> s.runtime != null && s.runtime > 0)
                .toList());

        if (pool.size() < 4) return null;

        Collections.shuffle(pool, rnd);
        List<MovieSeed> pick4 = pool.subList(0, 4);

        MovieSeed correct = longest
                ? pick4.stream().max(Comparator.comparingInt(s -> s.runtime)).orElse(null)
                : pick4.stream().min(Comparator.comparingInt(s -> s.runtime)).orElse(null);

        if (correct == null) return null;
        if (!usedCorrectTitles.add(correct.title.toLowerCase(Locale.ROOT))) return null;

        String entityKey = (longest ? "R_MAX:" : "R_MIN:") + correct.id;
        if (!usedEntities.add(entityKey)) return null;

        List<String> options = new ArrayList<>(pick4.stream().map(s -> s.title).toList());
        if (new HashSet<>(options).size() != 4) return null;

        String q = longest
                ? "Which of the following movies has the longest runtime?"
                : "Which of the following movies has the shortest runtime?";

        return new FullQuestion(q, correct.title, options);
    }

    // =========================
    // KEYWORD (from correct overview, validated)
    // - ensures distractors do NOT contain the keyword
    // =========================
    private FullQuestion buildKeywordQuestion(
            List<MovieSeed> seeds,
            Set<String> usedEntities,
            Set<String> usedCorrectTitles,
            int maxYear
    ) {
        Random rnd = new Random();

        List<MovieSeed> pool = new ArrayList<>(seeds.stream()
                .filter(s -> s != null && s.title != null && s.overview != null && s.year != null && s.year <= maxYear)
                .toList());

        if (pool.size() < 4) return null;

        Collections.shuffle(pool, rnd);
        MovieSeed correct = pool.get(0);
        if (!usedCorrectTitles.add(correct.title.toLowerCase(Locale.ROOT))) return null;

        String kw = pickGoodKeyword(correct.overview);
        if (kw == null) return null;

        String kwLower = kw.toLowerCase(Locale.ROOT);
        String entityKey = "K:" + kwLower;
        if (!usedEntities.add(entityKey)) return null;

        // verify keyword actually exists in correct overview
        if (correct.overview == null ||
                !correct.overview.toLowerCase(Locale.ROOT).contains(kwLower)) {
            return null;
        }

        // distractors MUST NOT contain keyword (avoid ambiguity)
        List<MovieSeed> distractors = new ArrayList<>(pool.stream()
                .filter(s -> !s.title.equalsIgnoreCase(correct.title))
                .filter(s -> s.overview == null || !s.overview.toLowerCase(Locale.ROOT).contains(kwLower))
                .toList());

        if (distractors.size() < 3) return null;

        Collections.shuffle(distractors, rnd);
        List<MovieSeed> d3 = distractors.subList(0, 3);

        List<String> options = new ArrayList<>();
        options.add(correct.title);
        options.add(d3.get(0).title);
        options.add(d3.get(1).title);
        options.add(d3.get(2).title);

        if (new HashSet<>(options).size() != 4) return null;

        String q = "Which movie's plot mentions \"" + kw + "\"?";
        return new FullQuestion(q, correct.title, options);
    }

    private String pickGoodKeyword(String overview) {
        if (overview == null) return null;

        String text = overview.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        String[] parts = text.split("\\s+");

        Set<String> stop = new HashSet<>(Arrays.asList(
                "the","and","that","this","with","from","into","over","when","then","than",
                "their","they","them","him","her","his","she","you","your","who","what",
                "where","why","how","after","before","during","while","about","through",
                "will","must","onto","upon","under","between","within","without",
                "movie","film","story","years","year","life","finds","find","sets","set",
                "really","always","never","around","across","another","already","raised",
                "involving","return","original","forced","forcing"
        ));

        List<String> candidates = new ArrayList<>();
        for (String w : parts) {
            if (w == null) continue;
            if (w.length() < 5) continue;
            if (w.length() > 14) continue;
            if (stop.contains(w)) continue;
            if (w.chars().anyMatch(Character::isDigit)) continue;

            // very light "quality" check: must include at least one vowel
            if (!w.matches(".*[aeiou].*")) continue;

            candidates.add(w);
        }

        if (candidates.isEmpty()) return null;
        Collections.shuffle(candidates, new Random());
        return candidates.get(0);
    }

    // ==========================================================
    // HF paraphrase (safe: only text)
    // ==========================================================
    private List<String> paraphraseQuestions(List<String> questions) throws Exception {
        if (questions == null || questions.isEmpty()) return null;

        String prompt =
                "Rewrite each quiz question to be more varied and natural, without changing meaning.\n" +
                        "Return ONLY valid JSON array of strings (same length and order). No markdown.\n\n" +
                        objectMapper.writeValueAsString(questions);

        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", prompt);

        Map<String, Object> params = new HashMap<>();
        params.put("max_new_tokens", 600);
        params.put("temperature", 0.6);
        params.put("return_full_text", false);
        payload.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, "application/json");
        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

        RestTemplate rt = restTemplateWithTimeouts();

        ResponseEntity<String> response;
        try {
            response = rt.exchange(modelUrl, HttpMethod.POST, request, String.class);
        } catch (RestClientException ex) {
            throw new RuntimeException("HF request failed: " + ex.getMessage());
        }

        String body = response.getBody();
        if (body == null || body.isBlank()) throw new IllegalStateException("Empty HF response");
        if (body.contains("\"estimated_time\"") || body.contains("\"error\"")) {
            throw new RuntimeException("HF model loading or error: " + body);
        }

        String generated = extractGeneratedText(body);
        String jsonArray = extractJsonArray(generated);

        JsonNode arr = objectMapper.readTree(jsonArray);
        if (!arr.isArray()) return null;

        List<String> out = new ArrayList<>();
        for (JsonNode n : arr) out.add(n.asText());
        return out;
    }

    private String extractGeneratedText(String hfBody) throws Exception {
        JsonNode node = objectMapper.readTree(hfBody);

        if (node.isArray() && node.size() > 0) {
            JsonNode first = node.get(0);
            JsonNode gt = first.get("generated_text");
            if (gt != null && gt.isTextual()) return gt.asText();
        }

        if (node.isObject()) {
            JsonNode gt = node.get("generated_text");
            if (gt != null && gt.isTextual()) return gt.asText();
        }

        throw new IllegalStateException("Unknown HF format: " + hfBody);
    }

    private String extractJsonArray(String text) {
        if (text == null) throw new IllegalStateException("No generated text");

        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalStateException("AI did not return JSON array. Text: " + text);
        }
        return text.substring(start, end + 1).trim();
    }

    // ==========================================================
    // Helpers
    // ==========================================================
    private Integer extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try { return Integer.parseInt(releaseDate.substring(0, 4)); }
        catch (Exception e) { return null; }
    }

    private List<Long> idsLong(List<PreferenceScoreDto> list, int limit) {
        if (list == null) return List.of();
        return list.stream()
                .filter(Objects::nonNull)
                .map(PreferenceScoreDto::getId)
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
    }

    private List<Integer> idsInt(List<PreferenceScoreDto> list, int limit) {
        if (list == null) return List.of();
        List<Integer> out = new ArrayList<>();
        for (PreferenceScoreDto p : list) {
            if (p == null || p.getId() == null) continue;
            out.add(p.getId().intValue());
            if (out.size() >= limit) break;
        }
        return out;
    }

    private enum QuestionType {
        ACTOR, DIRECTOR,
        GENRE, GENRE_NEG,
        YEAR, NEWEST, OLDEST,
        KEYWORD,
        POPULARITY_MAX, POPULARITY_MIN,
        RUNTIME_MAX, RUNTIME_MIN
    }

    private static class MovieSeed {
        long id;
        String title;
        String overview;
        Integer year;
        double popularity;

        Integer runtime; // minutes (nullable)

        Set<Integer> genreIds = new HashSet<>();
        Map<Integer, String> genreNames = new HashMap<>();

        Set<Long> castIds = new HashSet<>();
        Map<Long, String> castNames = new HashMap<>();

        Long directorId;
        String directorName;
    }
}
