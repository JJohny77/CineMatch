package com.cinematch.backend.service;

import com.cinematch.backend.dto.AudienceEngagementResponse;
import com.cinematch.backend.dto.StarPowerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final TmdbService tmdbService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(KpiService.class);

    // ==========================================================
    // 1) STAR POWER ΓΙΑ ΣΥΓΚΕΚΡΙΜΕΝΗ ΤΑΙΝΙΑ (movieId)
    //    Βασίζεται στους 5 top-billed actors του cast
    // ==========================================================
    public StarPowerResponse calculateStarPower(Long movieId) {

        try {
            // 1️⃣ Credits της ταινίας
            String creditsJson = tmdbService.fetchFromTmdb(
                    "/movie/" + movieId + "/credits",
                    Map.of("language", "en-US")
            );

            Map<String, Object> credits = objectMapper.readValue(creditsJson, Map.class);
            List<Map<String, Object>> cast = (List<Map<String, Object>>) credits.get("cast");

            if (cast == null || cast.isEmpty()) {
                return new StarPowerResponse(movieId, 0);
            }

            // 2️⃣ Κρατάμε τους 5 πρώτους βάσει "order" (top billed)
            List<Map<String, Object>> topCast = cast.stream()
                    .sorted(Comparator.comparingInt(a ->
                            ((Number) a.getOrDefault("order", 999)).intValue()
                    ))
                    .limit(5)
                    .toList();

            double totalScore = 0.0;
            int consideredActors = 0;

            // 3️⃣ Υπολογισμός score για κάθε ηθοποιό
            for (Map<String, Object> actor : topCast) {
                Integer actorId = (Integer) actor.get("id");
                if (actorId == null) continue;

                consideredActors++;

                // 3.1 Λεπτομέρειες ηθοποιού (popularity)
                String detailsJson = tmdbService.fetchFromTmdb(
                        "/person/" + actorId,
                        Map.of("language", "en-US")
                );
                Map<String, Object> details = objectMapper.readValue(detailsJson, Map.class);

                double popularity = details.get("popularity") != null
                        ? ((Number) details.get("popularity")).doubleValue()
                        : 0.0;
                // Κανονικοποίηση 0–100 (λίγο «γενναιόδωρη» για γνωστούς ηθοποιούς)
                double popularityScore = Math.min(100.0, popularity * 4.0);

                // 3.2 Credits καριέρας
                String actorCreditsJson = tmdbService.fetchFromTmdb(
                        "/person/" + actorId + "/movie_credits",
                        Map.of("language", "en-US")
                );
                Map<String, Object> actorCredits = objectMapper.readValue(actorCreditsJson, Map.class);
                List<Map<String, Object>> actorCastList =
                        (List<Map<String, Object>>) actorCredits.get("cast");

                int movieCount = actorCastList != null ? actorCastList.size() : 0;
                double careerScore = Math.min(100.0, movieCount * 1.0);

                // 3.3 Proxy «βραβείων» = πόσες ταινίες με vote_average > 7.5
                int awardProxy = 0;
                if (actorCastList != null) {
                    for (Map<String, Object> m : actorCastList) {
                        if (m.get("vote_average") != null) {
                            double v = ((Number) m.get("vote_average")).doubleValue();
                            if (v >= 7.5) awardProxy++;
                        }
                    }
                }
                double awardScore = Math.min(100.0, awardProxy * 4.0);

                // 3.4 Τελικό score ηθοποιού (weights: 50% popularity, 30% career, 20% awards)
                double actorScore =
                        (popularityScore * 0.5) +
                                (careerScore * 0.3) +
                                (awardScore * 0.2);

                totalScore += actorScore;
            }

            // 4️⃣ Τελικό Star Power της ταινίας = μέσος όρος των top ηθοποιών
            int finalScore = 0;
            if (consideredActors > 0) {
                finalScore = (int) Math.round(Math.min(100.0, totalScore / consideredActors));
            }

            return new StarPowerResponse(movieId, finalScore);

        } catch (Exception e) {
            logger.error("Failed to compute star power for movie {}", movieId, e);
            throw new RuntimeException("Failed to compute Star Power Index");
        }
    }

    // ==========================================================
    // 2) AUDIENCE ENGAGEMENT (όπως το είχες – ΔΕΝ το πειράζουμε)
    // ==========================================================
    public AudienceEngagementResponse calculateAudienceEngagement(Long movieId) {
        try {
            String json = tmdbService.fetchFromTmdb(
                    "/movie/" + movieId,
                    Map.of("language", "en-US")
            );

            Map<String, Object> movie = objectMapper.readValue(json, Map.class);

            int voteCount = movie.get("vote_count") != null
                    ? ((Number) movie.get("vote_count")).intValue()
                    : 0;

            double voteAverage = movie.get("vote_average") != null
                    ? ((Number) movie.get("vote_average")).doubleValue()
                    : 0.0;

            double popularity = movie.get("popularity") != null
                    ? ((Number) movie.get("popularity")).doubleValue()
                    : 0.0;

            double normVoteCount = Math.min(1.0, voteCount / 50000.0);
            double normPopularity = Math.min(1.0, popularity / 300.0);
            double normVoteAverage = voteAverage / 10.0;

            double engagement =
                    (normVoteCount * 50) +
                            (normVoteAverage * 30) +
                            (normPopularity * 20);

            int finalScore = (int) Math.round(Math.min(100, engagement));

            return new AudienceEngagementResponse(movieId, finalScore);

        } catch (Exception e) {
            logger.error("Failed to compute Audience Engagement for movie {}", movieId, e);
            throw new RuntimeException("Failed to compute audience engagement score");
        }
    }
}
