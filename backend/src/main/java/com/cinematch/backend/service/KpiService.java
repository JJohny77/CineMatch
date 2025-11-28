package com.cinematch.backend.service;

import com.cinematch.backend.dto.StarPowerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.cinematch.backend.dto.AudienceEngagementResponse;


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
    public AudienceEngagementResponse calculateAudienceEngagement(Long movieId) {
        try {
            // 1️⃣ Call TMDb movie endpoint
            String json = tmdbService.fetchFromTmdb(
                    "/movie/" + movieId,
                    Map.of("language", "en-US")
            );

            Map<String, Object> movie = objectMapper.readValue(json, Map.class);

            // 2️⃣ Extract needed fields (with safe defaults)
            int voteCount = movie.get("vote_count") != null
                    ? ((Number) movie.get("vote_count")).intValue()
                    : 0;

            double voteAverage = movie.get("vote_average") != null
                    ? ((Number) movie.get("vote_average")).doubleValue()
                    : 0.0;

            double popularity = movie.get("popularity") != null
                    ? ((Number) movie.get("popularity")).doubleValue()
                    : 0.0;

            // 3️⃣ Normalization helpers
            double normVoteCount = Math.min(1.0, voteCount / 50000.0);   // max 50k
            double normPopularity = Math.min(1.0, popularity / 300.0);    // max 300

            double normVoteAverage = voteAverage / 10.0; // since max is 10

            // 4️⃣ Weighted formula (50/30/20)
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

