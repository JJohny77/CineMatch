package com.cinematch.backend.service.ai;

import com.cinematch.backend.dto.MovieResultDto;
import com.cinematch.backend.dto.PreferenceScoreDto;
import com.cinematch.backend.dto.UserPreferencesResponseDto;
import com.cinematch.backend.quiz.service.FullQuestion;
import com.cinematch.backend.service.TmdbService;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuizGenerator {

    private final ObjectMapper objectMapper;
    private final TmdbService tmdbService;

    @Value("${huggingface.api.key:}")
    private String apiKey;

    // OpenAI-compatible chat completions endpoint (HF router)
    @Value("${huggingface.quiz.model-url:https://router.huggingface.co/v1/chat/completions}")
    private String modelUrl;

    // Model id on HF
    @Value("${huggingface.quiz.model-name:mistralai/Mistral-7B-Instruct-v0.2}")
    private String modelName;

    private static final int HTTP_TIMEOUT_MS = 12000;

    // =========================================================
    // PUBLIC
    // =========================================================
    public List<FullQuestion> generateFullQuestions(
            UserPreferencesResponseDto prefs,
            List<MovieResultDto> candidates,
            int desiredCount
    ) {
        if (desiredCount <= 0) return List.of();

        // 1) Always ensure we have a usable pool
        List<MovieResultDto> pool = new ArrayList<>(candidates == null ? List.of() : candidates);
        pool.removeIf(Objects::isNull);
        pool.removeIf(m -> m.getTitle() == null || m.getTitle().isBlank());

        if (pool.isEmpty()) {
            return localFallback(prefs, List.of(), desiredCount);
        }

        // 2) Attempt AI generation (3 tries)
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                List<FullQuestion> ai = generateViaHf(prefs, pool, desiredCount);

                if (ai != null && ai.size() >= desiredCount) {
                    return ai.subList(0, desiredCount);
                }

                if (ai != null && !ai.isEmpty()) {
                    List<FullQuestion> topUp = localFallback(prefs, pool, desiredCount - ai.size());
                    List<FullQuestion> merged = new ArrayList<>(ai);
                    merged.addAll(topUp);
                    return merged;
                }

            } catch (Exception e) {
                log.warn("AI quiz generation failed (attempt {}): {}", attempt, e.getMessage());
            }
        }

        // 3) Final fallback
        return localFallback(prefs, pool, desiredCount);
    }

    // =========================================================
    // HF GENERATION (OpenAI-compatible)
    // =========================================================
    private List<FullQuestion> generateViaHf(UserPreferencesResponseDto prefs, List<MovieResultDto> pool, int desiredCount) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("HUGGINGFACE_API_KEY is missing");
        }

        // Keep prompt light (avoid huge payload)
        List<MovieResultDto> contextMovies = pickContextMovies(pool, 28);

        String prompt = buildPrompt(prefs, contextMovies, desiredCount);

        RestTemplate restTemplate = buildRestTemplate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelName);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You generate high-quality, non-duplicate quiz questions. Output ONLY valid JSON."
        ));
        messages.add(Map.of(
                "role", "user",
                "content", prompt
        ));
        payload.put("messages", messages);

        payload.put("temperature", 0.9);
        payload.put("max_tokens", 900);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp;
        try {
            resp = restTemplate.exchange(modelUrl, HttpMethod.POST, req, String.class);
        } catch (RestClientException ex) {
            throw new RuntimeException("HF request failed: " + ex.getMessage(), ex);
        }

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("HF non-2xx response: " + resp.getStatusCode());
        }

        // HF(OpenAI-compatible) -> { choices:[ { message:{content:"..."} } ] }
        String content = extractChatContent(resp.getBody());
        if (content == null || content.isBlank()) {
            throw new RuntimeException("HF returned empty content");
        }

        String json = extractJsonObject(content);
        if (json == null) {
            throw new RuntimeException("Could not extract JSON from HF content");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("HF JSON parse failed");
        }

        JsonNode arr = root.get("questions");
        if (arr == null || !arr.isArray()) {
            throw new RuntimeException("HF JSON missing 'questions' array");
        }

        Set<String> seenQuestions = new HashSet<>();
        Map<String, Integer> correctAnswerCount = new HashMap<>();

        List<FullQuestion> out = new ArrayList<>();

        for (JsonNode q : arr) {

            String qt = safeText(q.get("questionText"));
            JsonNode optsNode = q.get("options");
            Integer correctIdx = safeInt(q.get("correctOptionIndex"));

            if (qt == null || qt.isBlank()) continue;
            if (optsNode == null || !optsNode.isArray() || optsNode.size() != 4) continue;
            if (correctIdx == null || correctIdx < 0 || correctIdx > 3) continue;

            List<String> opts = new ArrayList<>();
            for (JsonNode o : optsNode) {
                String t = o == null ? null : o.asText();
                if (t != null) t = t.trim();
                opts.add(t);
            }

            // validate options
            if (opts.stream().anyMatch(s -> s == null || s.isBlank())) continue;
            if (new HashSet<>(opts).size() != 4) continue;

            String correct = opts.get(correctIdx);
            if (correct == null || correct.isBlank()) continue;

            // anti-duplicate guards
            String sigQ = qt.trim().toLowerCase();
            if (!seenQuestions.add(sigQ)) continue;

            // Avoid “same correct answer everywhere”
            String ca = correct.trim().toLowerCase();
            int count = correctAnswerCount.getOrDefault(ca, 0);
            if (count >= 2 && desiredCount >= 8) {
                continue;
            }
            correctAnswerCount.put(ca, count + 1);

            out.add(new FullQuestion(qt.trim(), correct, opts));
            if (out.size() >= desiredCount) break;
        }

        if (out.size() < Math.min(4, desiredCount)) {
            throw new RuntimeException("HF produced too few valid questions");
        }

        return out;
    }

    // =========================================================
    // LOCAL FALLBACK (TMDB-based)
    // =========================================================
    private List<FullQuestion> localFallback(UserPreferencesResponseDto prefs, List<MovieResultDto> pool, int desiredCount) {

        List<MovieResultDto> base = new ArrayList<>(pool == null ? List.of() : pool);

        if (base.isEmpty()) {
            try {
                base = new ArrayList<>(
                        tmdbService.getTrendingMovies("day").stream().map(t -> {
                            MovieResultDto m = new MovieResultDto();
                            m.setId(t.getId().intValue());
                            m.setTitle(t.getTitle());
                            m.setOverview(t.getOverview());
                            m.setPoster_path(t.getPosterPath());
                            m.setRelease_date(t.getReleaseDate());
                            m.setPopularity(t.getPopularity());
                            return m;
                        }).toList()
                );
            } catch (Exception ignored) {}
        }

        base.removeIf(Objects::isNull);
        base.removeIf(m -> m.getTitle() == null || m.getTitle().isBlank());

        Random rnd = new Random();
        Collections.shuffle(base, rnd);

        Set<String> usedSignatures = new HashSet<>();
        Set<Integer> usedMovieIds = new HashSet<>();

        List<FullQuestion> out = new ArrayList<>();

        int tries = 0;
        while (out.size() < desiredCount && tries++ < 600) {

            int mode = tries % 3;

            if (mode == 0) {
                FullQuestion q = makeYearQuestion(base, usedSignatures, usedMovieIds, rnd);
                if (q != null) out.add(q);
            } else if (mode == 1) {
                FullQuestion q = makeKeywordOverviewQuestion(base, usedSignatures, usedMovieIds, rnd);
                if (q != null) out.add(q);
            } else {
                FullQuestion q = makePopularityQuestion(base, usedSignatures, usedMovieIds, rnd);
                if (q != null) out.add(q);
            }
        }

        tries = 0;
        while (out.size() < desiredCount && tries++ < 200) {
            FullQuestion q = makeOldestQuestion(base, usedSignatures, rnd);
            if (q != null) out.add(q);
        }

        return out;
    }

    // =========================================================
    // QUESTION TYPES
    // =========================================================
    private FullQuestion makeYearQuestion(List<MovieResultDto> base, Set<String> used, Set<Integer> usedMovieIds, Random rnd) {

        MovieResultDto correctMovie = pickMovieWithYear(base, usedMovieIds, rnd);
        if (correctMovie == null) return null;

        Integer year = extractYear(correctMovie.getRelease_date());
        if (year == null) return null;

        List<MovieResultDto> optionsMovies = pickDistinctMovies(base, 4, correctMovie, rnd);
        if (optionsMovies.size() != 4) return null;

        optionsMovies.set(0, correctMovie);
        Collections.shuffle(optionsMovies, rnd);

        List<String> options = optionsMovies.stream().map(MovieResultDto::getTitle).toList();
        String correct = correctMovie.getTitle();

        String question = "Ποια από τις παρακάτω ταινίες κυκλοφόρησε το " + year + ";";
        String sig = (question + options).toLowerCase();
        if (!used.add(sig)) return null;

        return new FullQuestion(question, correct, options);
    }

    private FullQuestion makeKeywordOverviewQuestion(List<MovieResultDto> base, Set<String> used, Set<Integer> usedMovieIds, Random rnd) {

        MovieResultDto correctMovie = pickMovieWithOverview(base, usedMovieIds, rnd);
        if (correctMovie == null) return null;

        String ov = correctMovie.getOverview();
        if (ov == null) return null;

        String keyword = pickKeyword(ov);
        if (keyword == null) return null;

        List<MovieResultDto> optionsMovies = pickDistinctMovies(base, 4, correctMovie, rnd);
        if (optionsMovies.size() != 4) return null;

        optionsMovies.set(0, correctMovie);
        Collections.shuffle(optionsMovies, rnd);

        List<String> options = optionsMovies.stream().map(MovieResultDto::getTitle).toList();
        String correct = correctMovie.getTitle();

        String question = "Σε ποια ταινία η υπόθεση περιλαμβάνει τη λέξη/έννοια: \"" + keyword + "\";";
        String sig = (question + options).toLowerCase();
        if (!used.add(sig)) return null;

        return new FullQuestion(question, correct, options);
    }

    private FullQuestion makePopularityQuestion(List<MovieResultDto> base, Set<String> used, Set<Integer> usedMovieIds, Random rnd) {

        List<MovieResultDto> optionsMovies = pickDistinctMovies(base, 4, null, rnd);
        if (optionsMovies.size() != 4) return null;

        MovieResultDto correctMovie = optionsMovies.stream()
                .max(Comparator.comparingDouble(this::popularityOf))
                .orElse(null);

        if (correctMovie == null) return null;

        usedMovieIds.add(correctMovie.getId());

        List<String> options = optionsMovies.stream().map(MovieResultDto::getTitle).toList();
        String correct = correctMovie.getTitle();

        String question = "Ποια από τις παρακάτω ταινίες είναι η πιο δημοφιλής;";
        String sig = (question + options + correct).toLowerCase();
        if (!used.add(sig)) return null;

        return new FullQuestion(question, correct, options);
    }

    private FullQuestion makeOldestQuestion(List<MovieResultDto> base, Set<String> used, Random rnd) {

        List<MovieResultDto> optionsMovies = pickDistinctMovies(base, 4, null, rnd);
        if (optionsMovies.size() != 4) return null;

        MovieResultDto correctMovie = optionsMovies.stream()
                .min(Comparator.comparing(m -> {
                    Integer y = extractYear(m.getRelease_date());
                    return y == null ? 99999 : y;
                }))
                .orElse(null);

        if (correctMovie == null) return null;

        List<String> options = optionsMovies.stream().map(MovieResultDto::getTitle).toList();
        String correct = correctMovie.getTitle();

        String question = "Ποια από τις παρακάτω ταινίες είναι η πιο παλιά (πρώτη κυκλοφορία);";
        String sig = (question + options + correct).toLowerCase();
        if (!used.add(sig)) return null;

        return new FullQuestion(question, correct, options);
    }

    // =========================================================
    // PICKERS
    // =========================================================
    private MovieResultDto pickMovieWithYear(List<MovieResultDto> base, Set<Integer> usedMovieIds, Random rnd) {
        for (int i = 0; i < 80; i++) {
            MovieResultDto m = base.get(rnd.nextInt(base.size()));
            if (m.getId() <= 0) continue;
            if (usedMovieIds.contains(m.getId())) continue;

            Integer y = extractYear(m.getRelease_date());
            if (y == null) continue;

            int maxYear = Year.now().getValue();
            if (y > maxYear) continue;

            usedMovieIds.add(m.getId());
            return m;
        }
        return null;
    }

    private MovieResultDto pickMovieWithOverview(List<MovieResultDto> base, Set<Integer> usedMovieIds, Random rnd) {
        for (int i = 0; i < 100; i++) {
            MovieResultDto m = base.get(rnd.nextInt(base.size()));
            if (m.getId() <= 0) continue;
            if (usedMovieIds.contains(m.getId())) continue;
            if (m.getOverview() == null || m.getOverview().trim().length() < 80) continue;

            usedMovieIds.add(m.getId());
            return m;
        }
        return null;
    }

    private List<MovieResultDto> pickDistinctMovies(List<MovieResultDto> base, int count, MovieResultDto mustInclude, Random rnd) {

        List<MovieResultDto> copy = new ArrayList<>(base);
        Collections.shuffle(copy, rnd);

        LinkedHashMap<Integer, MovieResultDto> unique = new LinkedHashMap<>();
        for (MovieResultDto m : copy) {
            if (m == null || m.getTitle() == null || m.getTitle().isBlank()) continue;
            unique.putIfAbsent(m.getId(), m);
            if (unique.size() >= 60) break;
        }

        List<MovieResultDto> list = new ArrayList<>(unique.values());
        if (mustInclude != null) {
            list.removeIf(x -> x.getId() == mustInclude.getId());
            list.add(0, mustInclude);
        }

        List<MovieResultDto> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (MovieResultDto m : list) {
            if (m == null) continue;
            if (!seen.add(m.getId())) continue;
            out.add(m);
            if (out.size() == count) break;
        }
        return out.size() == count ? out : List.of();
    }

    private List<MovieResultDto> pickContextMovies(List<MovieResultDto> pool, int max) {
        List<MovieResultDto> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, new Random());

        List<MovieResultDto> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (MovieResultDto m : copy) {
            if (m == null) continue;
            if (m.getTitle() == null || m.getTitle().isBlank()) continue;
            if (!seen.add(m.getId())) continue;
            out.add(m);
            if (out.size() >= max) break;
        }
        return out;
    }

    // =========================================================
    // PROMPT
    // =========================================================
    private String buildPrompt(UserPreferencesResponseDto prefs, List<MovieResultDto> contextMovies, int desiredCount) {

        List<PreferenceScoreDto> g = prefs != null ? safeList(prefs.getTopGenres()) : List.of();
        List<PreferenceScoreDto> a = prefs != null ? safeList(prefs.getTopActors()) : List.of();
        List<PreferenceScoreDto> d = prefs != null ? safeList(prefs.getTopDirectors()) : List.of();

        StringBuilder sb = new StringBuilder();

        sb.append("Return JSON in this exact schema:\n");
        sb.append("{\"questions\":[{\"questionText\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctOptionIndex\":0,\"movieId\":123}]}\n\n");

        sb.append("Rules:\n");
        sb.append("- Create ").append(desiredCount).append(" questions.\n");
        sb.append("- NO duplicates (questionText must be unique).\n");
        sb.append("- Options must be exactly 4 unique strings.\n");
        sb.append("- correctOptionIndex must match the correct option.\n");
        sb.append("- Prefer Greek wording for the questionText.\n");
        sb.append("- Use ONLY these movies (titles) as options, do not invent random movies.\n");
        sb.append("- Mix question types (year, plot keyword, popularity, genre-like phrasing).\n\n");

        sb.append("User preference IDs (for guidance only):\n");
        sb.append("topGenres=").append(safeJson(g)).append("\n");
        sb.append("topActors=").append(safeJson(a)).append("\n");
        sb.append("topDirectors=").append(safeJson(d)).append("\n\n");

        sb.append("Movies you can use (id,title,release_date,overview snippet, popularity):\n");
        for (MovieResultDto m : contextMovies) {
            sb.append("- {");
            sb.append("\"id\":").append(m.getId()).append(",");
            sb.append("\"title\":\"").append(escape(m.getTitle())).append("\",");
            sb.append("\"release_date\":\"").append(escape(m.getRelease_date())).append("\",");
            sb.append("\"popularity\":").append(popularityOf(m)).append(",");
            sb.append("\"overview\":\"").append(escape(shortOverview(m.getOverview()))).append("\"");
            sb.append("}\n");
        }

        sb.append("\nIMPORTANT: Output ONLY JSON. No markdown, no explanations.");
        return sb.toString();
    }

    // =========================================================
    // HTTP + PARSING HELPERS
    // =========================================================
    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(HTTP_TIMEOUT_MS);
        factory.setReadTimeout(HTTP_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    private String extractChatContent(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode msg = choices.get(0).get("message");
                if (msg != null && msg.get("content") != null) {
                    return msg.get("content").asText();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractJsonObject(String text) {
        if (text == null) return null;

        Pattern p = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group();

        return null;
    }

    // =========================================================
    // TEXT/NUM HELPERS
    // =========================================================
    private Integer extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try { return Integer.parseInt(releaseDate.substring(0, 4)); }
        catch (Exception e) { return null; }
    }

    private String pickKeyword(String overview) {
        if (overview == null) return null;

        String clean = overview.replaceAll("[^A-Za-zΑ-Ωα-ω0-9\\s]", " ");
        String[] parts = clean.split("\\s+");

        List<String> candidates = new ArrayList<>();
        for (String w : parts) {
            if (w == null) continue;
            w = w.trim();
            if (w.length() < 6) continue;

            String lw = w.toLowerCase();
            if (Set.of(
                    "because","within","without","before","after","where","which","their","there","about","these","those",
                    "στην","στον","αυτή","αυτό","ένας","μια","όταν","γιατί","μετά","πριν","από"
            ).contains(lw)) continue;

            candidates.add(w);
        }

        if (candidates.isEmpty()) return null;

        Collections.shuffle(candidates);
        return candidates.get(0);
    }

    private String shortOverview(String ov) {
        if (ov == null) return "";
        String s = ov.trim();
        if (s.length() <= 160) return s;
        return s.substring(0, 160) + "...";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String safeText(JsonNode n) {
        if (n == null) return null;
        String s = n.asText();
        return s == null ? null : s.trim();
    }

    private Integer safeInt(JsonNode n) {
        if (n == null) return null;
        try { return n.asInt(); }
        catch (Exception e) { return null; }
    }

    private String safeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return "[]"; }
    }

    private <T> List<T> safeList(List<T> in) {
        return in == null ? List.of() : in;
    }

    /**
     * Works whether popularity is Double or primitive double.
     * (Avoids "== null" compile errors + avoids NPE on unboxing.)
     */
    private double popularityOf(MovieResultDto m) {
        if (m == null) return 0.0;
        try {
            Double p = m.getPopularity();
            return p == null ? 0.0 : p;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
