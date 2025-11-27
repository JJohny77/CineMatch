package com.cinematch.backend.service;

import com.cinematch.backend.dto.StarPowerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final TmdbService tmdbService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(KpiService.class);

    public StarPowerResponse calculateStarPower(Long actorId) {

        try {
            // 1️⃣ Get person details
            String detailsJson = tmdbService.fetchFromTmdb(
                    "/person/" + actorId,
                    Map.of("language", "en-US")
            );

            Map<String, Object> details = objectMapper.readValue(detailsJson, Map.class);
            Double popularity = details.get("popularity") != null
                    ? ((Number) details.get("popularity")).doubleValue()
                    : 0.0;

            // 2️⃣ Get movie credits
            String creditsJson = tmdbService.fetchFromTmdb(
                    "/person/" + actorId + "/movie_credits",
                    Map.of("language", "en-US")
            );

            Map<String, Object> credits = objectMapper.readValue(creditsJson, Map.class);
            List<Map<String, Object>> cast = (List<Map<String, Object>>) credits.get("cast");

            int castCount = cast != null ? cast.size() : 0;

            // Award count proxy = movies with vote_average > 7.5
            int awardProxy = 0;
            if (cast != null) {
                for (Map<String, Object> movie : cast) {
                    if (movie.get("vote_average") != null) {
                        double vote = ((Number) movie.get("vote_average")).doubleValue();
                        if (vote > 7.5) awardProxy++;
                    }
                }
            }

            // 3️⃣ Weighted score computation (simple normalized formula)
            double score =
                    (popularity * 2) +
                            (castCount * 1.5) +
                            (awardProxy * 3);

            // Normalize to 0–100
            int finalScore = (int) Math.min(100, Math.round(score / 10));

            return new StarPowerResponse(actorId, finalScore);

        } catch (Exception e) {
            logger.error("Failed to compute star power for actor {}", actorId, e);
            throw new RuntimeException("Failed to compute Star Power Index");
        }
    }
}
