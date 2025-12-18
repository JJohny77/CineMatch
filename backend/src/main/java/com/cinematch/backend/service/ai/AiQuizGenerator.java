package com.cinematch.backend.service.ai;

import com.cinematch.backend.dto.MovieResultDto;
import com.cinematch.backend.dto.UserPreferencesResponseDto;
import com.cinematch.backend.quiz.dto.AiQuizQuestionDto;
import com.cinematch.backend.quiz.service.FullQuestion;
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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuizGenerator {

    private final ObjectMapper objectMapper;

    @Value("${huggingface.api.key:}")
    private String apiKey;

    @Value("${huggingface.quiz.model-url:https://router.huggingface.co/hf-inference/models/mistralai/Mistral-7B-Instruct-v0.2}")
    private String modelUrl;

    // Τuning (μπορείς να τα αλλάξεις στο application.properties αν θες)
    @Value("${huggingface.quiz.max-new-tokens:900}")
    private int maxNewTokens;

    @Value("${huggingface.quiz.temperature:0.9}")
    private double temperature;

    @Value("${huggingface.quiz.top-p:0.95}")
    private double topP;

    @Value("${huggingface.quiz.repetition-penalty:1.15}")
    private double repetitionPenalty;

    private static final int MAX_MOVIES_IN_PROMPT = 28; // για να μην γίνεται τεράστιο το prompt
    private static final int OVERVIEW_MAX_CHARS = 220;
    private static final Pattern NON_WORDS = Pattern.compile("[^\\p{L}\\p{N}]+");

    // =========================================================
    // PUBLIC API
    // =========================================================
    public List<FullQuestion> generateFullQuestions(UserPreferencesResponseDto prefs,
                                                    List<MovieResultDto> candidates,
                                                    int count) {

        if (count <= 0) return List.of();

        List<MovieResultDto> pool = sanitizeCandidates(candidates);
        if (pool.isEmpty()) return List.of();

        // μέσα σε ένα quiz: δεν αφήνουμε duplicates
        Set<String> usedFingerprints = new HashSet<>();
        List<FullQuestion> out = new ArrayList<>();

        // Αν δεν έχει key/model → μόνο fallback
        boolean canUseAi = apiKey != null && !apiKey.isBlank() && modelUrl != null && !modelUrl.isBlank();

        int attempts = canUseAi ? 3 : 0;

        for (int attempt = 1; attempt <= attempts && out.size() < count; attempt++) {

            int need = count - out.size();

            // oversampling: ζητάμε παραπάνω για να κόψουμε duplicates/σκάρτα
            int askFor = Math.max(need * 3, 12);

            String prompt = buildPrompt(prefs, pool, askFor, usedFingerprints);

            String raw;
            try {
                raw = callHuggingFace(prompt);
            } catch (Exception e) {
                log.warn("AI quiz generation failed (attempt {}): {}", attempt, e.getMessage());
                continue;
            }

            List<FullQuestion> parsed = parseToFullQuestions(raw);

            for (FullQuestion fq : parsed) {
                if (fq == null) continue;

                if (!isValidFullQuestion(fq)) continue;

                String fp = fingerprint(fq.question());
                if (usedFingerprints.contains(fp)) continue;

                usedFingerprints.add(fp);
                out.add(fq);

                if (out.size() >= count) break;
            }
        }

        // fallback fill
        if (out.size() < count) {
            int remaining = count - out.size();
            out.addAll(generateFallback(pool, remaining, usedFingerprints));
        }

        // τελική ασφάλεια
        if (out.size() > count) {
            return out.subList(0, count);
        }
        return out;
    }

    // =========================================================
    // HF CALL
    // =========================================================
    private String callHuggingFace(String prompt) {

        RestTemplate restTemplate = buildRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_new_tokens", maxNewTokens);
        parameters.put("temperature", temperature);
        parameters.put("top_p", topP);
        parameters.put("repetition_penalty", repetitionPenalty);
        parameters.put("return_full_text", false);

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", prompt);
        body.put("parameters", parameters);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    modelUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new RuntimeException("HF error: " + resp.getStatusCode());
            }

            // HF Inference επιστρέφει συνήθως array με generated_text
            JsonNode root = objectMapper.readTree(resp.getBody());

            if (root.isArray() && root.size() > 0 && root.get(0).has("generated_text")) {
                return root.get(0).get("generated_text").asText();
            }

            // fallback: ίσως έρθει plain string / object
            if (root.isTextual()) return root.asText();

            return resp.getBody();

        } catch (RestClientException ex) {
            throw new RuntimeException("HF request failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("HF parse failed: " + ex.getMessage(), ex);
        }
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }

    // =========================================================
    // PROMPT
    // =========================================================
    private String buildPrompt(UserPreferencesResponseDto prefs,
                               List<MovieResultDto> pool,
                               int askFor,
                               Set<String> alreadyUsed) {

        // μικρό, στοχευμένο pool στο prompt
        List<MovieResultDto> sampled = sampleMovies(pool, MAX_MOVIES_IN_PROMPT);

        StringBuilder sb = new StringBuilder();

        sb.append("""
Είσαι generator για CINEMATCH QUIZ.
Θα σου δώσω ταινίες (id, title, year, overview). Αυτές είναι ήδη επιλεγμένες ώστε να ταιριάζουν στα preferences του χρήστη.
Θέλω να φτιάξεις ΠΟΙΚΙΛΕΣ ερωτήσεις, ΟΧΙ διπλότυπες, ΟΧΙ παρόμοιες.

ΑΠΑΙΤΗΣΕΙΣ (αυστηρά):
1) Απάντησε ΜΟΝΟ με ένα JSON array (χωρίς markdown, χωρίς extra κείμενο).
2) Κάθε στοιχείο να έχει ΑΚΡΙΒΩΣ fields:
   - questionText (string, Ελληνικά)
   - options (array 4 strings)
   - correctOptionIndex (0..3)
   - movieId (number, το id της σωστής ταινίας)
3) options: ΑΚΡΙΒΩΣ 4, όλες διαφορετικές, και η σωστή επιλογή να βρίσκεται μέσα.
4) Να μην επαναλαμβάνονται τύποι ερώτησης. Χρησιμοποίησε μίξη από:
   - release year
   - “ποια ταινία έχει αυτή την πλοκή/λέξη-κλειδί”
   - director/actor ΜΟΝΟ αν προκύπτει με ασφάλεια από το overview (αλλιώς απόφυγέ το)
   - σύγκριση 2 ταινιών (“ποια κυκλοφόρησε νωρίτερα;” με 4 επιλογές)
5) Καμία ερώτηση να μην μοιάζει με άλλη. Καμία.

""");

        if (prefs != null) {
            sb.append("User preferences snapshot (για context):\n");
            sb.append("- topGenres: ").append(safeJson(prefs.getTopGenres())).append("\n");
            sb.append("- topActors: ").append(safeJson(prefs.getTopActors())).append("\n");
            sb.append("- topDirectors: ").append(safeJson(prefs.getTopDirectors())).append("\n\n");
        }

        if (alreadyUsed != null && !alreadyUsed.isEmpty()) {
            sb.append("ΜΗΝ φτιάξεις ερωτήσεις που μοιάζουν με αυτές (fingerprints):\n");
            int i = 0;
            for (String fp : alreadyUsed) {
                sb.append("- ").append(fp).append("\n");
                if (++i >= 8) break;
            }
            sb.append("\n");
        }

        sb.append("ΤΑΙΝΙΕΣ (χρησιμοποίησε ΜΟΝΟ αυτές για σωστές απαντήσεις):\n");

        for (MovieResultDto m : sampled) {
            Integer y = extractYear(m.getRelease_date());
            String ov = trim(m.getOverview(), OVERVIEW_MAX_CHARS);
            sb.append("- { id: ").append(m.getId())
                    .append(", title: \"").append(escape(m.getTitle())).append("\"")
                    .append(", year: ").append(y == null ? "null" : y)
                    .append(", overview: \"").append(escape(ov)).append("\" }\n");
        }

        sb.append("\n");
        sb.append("Ζητούμενο: Δώσε ").append(askFor).append(" ερωτήσεις.\n");
        sb.append("JSON array μόνο.\n");

        return sb.toString();
    }

    private String safeJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    // =========================================================
    // PARSING / VALIDATION
    // =========================================================
    private List<FullQuestion> parseToFullQuestions(String modelText) {

        if (modelText == null || modelText.isBlank()) return List.of();

        String json = extractJsonArray(modelText);
        if (json == null) return List.of();

        try {
            List<AiQuizQuestionDto> dtos = objectMapper.readValue(
                    json,
                    new TypeReference<List<AiQuizQuestionDto>>() {}
            );

            List<FullQuestion> out = new ArrayList<>();
            for (AiQuizQuestionDto dto : dtos) {
                if (dto == null) continue;
                if (dto.getQuestionText() == null || dto.getOptions() == null || dto.getCorrectOptionIndex() == null)
                    continue;

                List<String> opts = dto.getOptions().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();

                if (opts.size() != 4) continue;

                int idx = dto.getCorrectOptionIndex();
                if (idx < 0 || idx > 3) continue;

                // unique options
                if (new HashSet<>(opts).size() != 4) continue;

                String correct = opts.get(idx);

                out.add(new FullQuestion(dto.getQuestionText().trim(), correct, opts));
            }
            return out;

        } catch (Exception e) {
            log.warn("Failed to parse AI JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) return null;
        return text.substring(start, end + 1).trim();
    }

    private boolean isValidFullQuestion(FullQuestion fq) {
        try {
            if (fq.question() == null || fq.question().trim().length() < 12) return false;
            if (fq.options() == null || fq.options().size() != 4) return false;
            if (new HashSet<>(fq.options()).size() != 4) return false;
            if (fq.correctAnswer() == null || fq.correctAnswer().isBlank()) return false;
            return fq.options().contains(fq.correctAnswer());
        } catch (Exception e) {
            return false;
        }
    }

    private String fingerprint(String s) {
        if (s == null) return "";
        String x = s.toLowerCase(Locale.ROOT).trim();
        x = NON_WORDS.matcher(x).replaceAll(" ");
        x = x.replaceAll("\\s+", " ").trim();
        // κρατάμε αρχή για να δίνουμε “hint” στο AI χωρίς να εκθέτουμε όλο
        if (x.length() > 60) x = x.substring(0, 60);
        return x;
    }

    // =========================================================
    // FALLBACK (no-AI / fill gaps)
    // =========================================================
    private List<FullQuestion> generateFallback(List<MovieResultDto> pool,
                                                int count,
                                                Set<String> usedFingerprints) {

        List<MovieResultDto> safe = new ArrayList<>(pool);
        Collections.shuffle(safe, new Random());

        List<FullQuestion> out = new ArrayList<>();

        int safety = 0;
        while (out.size() < count && safety++ < 300) {

            MovieResultDto correct = pickMovieWithYear(safe);
            if (correct == null) break;

            FullQuestion q = buildYearQuestion(correct, safe);
            if (q == null) continue;

            String fp = fingerprint(q.question());
            if (usedFingerprints.contains(fp)) continue;

            usedFingerprints.add(fp);
            out.add(q);
        }

        // αν ακόμα λείπουν, κάνε keyword-from-overview
        safety = 0;
        while (out.size() < count && safety++ < 300) {

            MovieResultDto correct = pickMovieWithOverview(safe);
            if (correct == null) break;

            FullQuestion q = buildOverviewKeywordQuestion(correct, safe);
            if (q == null) continue;

            String fp = fingerprint(q.question());
            if (usedFingerprints.contains(fp)) continue;

            usedFingerprints.add(fp);
            out.add(q);
        }

        return out;
    }

    private FullQuestion buildYearQuestion(MovieResultDto correct, List<MovieResultDto> pool) {
        Integer y = extractYear(correct.getRelease_date());
        if (y == null) return null;

        List<MovieResultDto> decoys = pickDecoys(pool, correct, 3);
        if (decoys.size() < 3) return null;

        List<String> options = new ArrayList<>();
        options.add(correct.getTitle());
        for (MovieResultDto d : decoys) options.add(d.getTitle());

        Collections.shuffle(options);

        int idx = options.indexOf(correct.getTitle());
        if (idx < 0) return null;

        String question = "Ποια από τις παρακάτω ταινίες κυκλοφόρησε το " + y + ";";
        return new FullQuestion(question, correct.getTitle(), options);
    }

    private FullQuestion buildOverviewKeywordQuestion(MovieResultDto correct, List<MovieResultDto> pool) {
        String ov = correct.getOverview();
        if (ov == null || ov.trim().length() < 80) return null;

        String keyword = pickKeyword(ov);
        if (keyword == null) return null;

        List<MovieResultDto> decoys = pickDecoys(pool, correct, 3);
        if (decoys.size() < 3) return null;

        List<String> options = new ArrayList<>();
        options.add(correct.getTitle());
        for (MovieResultDto d : decoys) options.add(d.getTitle());

        Collections.shuffle(options);

        String question = "Σε ποια ταινία η υπόθεση περιλαμβάνει τη λέξη/έννοια: \"" + keyword + "\";";
        return new FullQuestion(question, correct.getTitle(), options);
    }

    private String pickKeyword(String overview) {
        String cleaned = overview.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        String[] parts = cleaned.split("\\s+");

        List<String> candidates = new ArrayList<>();
        for (String p : parts) {
            if (p == null) continue;
            String w = p.trim();
            if (w.length() < 7) continue;
            // αποφυγή πολύ κοινών
            String lw = w.toLowerCase(Locale.ROOT);
            if (lw.equals("because") || lw.equals("without") || lw.equals("between")) continue;
            candidates.add(w);
        }

        if (candidates.isEmpty()) return null;

        Collections.shuffle(candidates);
        return candidates.get(0);
    }

    private MovieResultDto pickMovieWithYear(List<MovieResultDto> pool) {
        for (MovieResultDto m : pool) {
            if (m == null) continue;
            if (m.getTitle() == null || m.getTitle().isBlank()) continue;
            Integer y = extractYear(m.getRelease_date());
            if (y != null && y <= Year.now().getValue()) return m;
        }
        return null;
    }

    private MovieResultDto pickMovieWithOverview(List<MovieResultDto> pool) {
        for (MovieResultDto m : pool) {
            if (m == null) continue;
            if (m.getTitle() == null || m.getTitle().isBlank()) continue;
            if (m.getOverview() != null && m.getOverview().trim().length() >= 90) return m;
        }
        return null;
    }

    private List<MovieResultDto> pickDecoys(List<MovieResultDto> pool, MovieResultDto correct, int n) {
        List<MovieResultDto> list = new ArrayList<>();
        for (MovieResultDto m : pool) {
            if (m == null) continue;
            if (m.getTitle() == null || m.getTitle().isBlank()) continue;
            if (m.getId() == correct.getId()) continue;
            list.add(m);
        }
        Collections.shuffle(list);
        return list.size() > n ? list.subList(0, n) : list;
    }

    // =========================================================
    // CANDIDATES / UTILS
    // =========================================================
    private List<MovieResultDto> sanitizeCandidates(List<MovieResultDto> candidates) {
        if (candidates == null) return List.of();

        int maxYear = Year.now().getValue() + 1;

        List<MovieResultDto> out = new ArrayList<>();
        for (MovieResultDto m : candidates) {
            if (m == null) continue;
            if (m.getTitle() == null || m.getTitle().isBlank()) continue;

            Integer y = extractYear(m.getRelease_date());
            if (y != null && y > maxYear) continue;

            // θέλουμε αρκετό overview για “στοχευμένες” ερωτήσεις
            if (m.getOverview() == null || m.getOverview().trim().length() < 40) continue;

            out.add(m);
        }

        // unique by id (κρατάμε σειρά)
        Map<Integer, MovieResultDto> unique = new LinkedHashMap<>();
        for (MovieResultDto m : out) {
            unique.putIfAbsent(m.getId(), m);
        }
        return new ArrayList<>(unique.values());
    }

    private List<MovieResultDto> sampleMovies(List<MovieResultDto> pool, int limit) {
        if (pool.size() <= limit) return pool;
        List<MovieResultDto> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, new Random());
        return copy.subList(0, limit);
    }

    private Integer extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try { return Integer.parseInt(releaseDate.substring(0, 4)); }
        catch (Exception e) { return null; }
    }

    private String trim(String s, int max) {
        if (s == null) return "";
        String x = s.trim().replaceAll("\\s+", " ");
        if (x.length() <= max) return x;
        return x.substring(0, max) + "...";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
