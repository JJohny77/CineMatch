package com.cinematch.backend.service.ai;

import com.cinematch.backend.dto.MovieResultDto;
import com.cinematch.backend.dto.MovieSearchResponse;
import com.cinematch.backend.dto.RecommendByMoodResponse;
import com.cinematch.backend.dto.SentimentResponse;
import com.cinematch.backend.model.ai.MovieSentiment;
import com.cinematch.backend.repository.MovieSentimentRepository;
import com.cinematch.backend.service.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MoodRecommendationService {

    private final SentimentService sentimentService;
    private final MovieSentimentRepository movieSentimentRepository;
    private final TmdbService tmdbService;

    public RecommendByMoodResponse recommendByMood(String text) {

        if (text == null || text.isBlank()) {
            return new RecommendByMoodResponse("neutral", 0.0, "neutral", List.of());
        }

        SentimentResponse sentiment = sentimentService.analyze(text);

        // Αν HF γυρίσει "loading", δεν μπλοκάρουμε MVP — πάμε neutral fallback.
        String label = sentiment.getSentiment();
        double score = sentiment.getScore();

        String tag = mapLabelToTag(label);
        if ("loading".equalsIgnoreCase(label)) {
            tag = "neutral";
        }

        int target = 12;

        // 1) Πρώτα από DB tagged movies
        Map<Integer, MovieResultDto> unique = new LinkedHashMap<>();

        List<MovieSentiment> tagged = movieSentimentRepository.findTop200ByTagOrderByScoreDesc(tag);

        if (tagged != null && !tagged.isEmpty()) {
            // παίρνουμε unique movieIds (με σειρά score desc)
            List<Long> ids = tagged.stream()
                    .map(MovieSentiment::getMovieId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(30)
                    .toList();

            for (Long id : ids) {
                try {
                    MovieResultDto card = tmdbService.getMovieCard(id);
                    if (card != null && card.getId() > 0) {
                        unique.putIfAbsent(card.getId(), card);
                    }
                } catch (Exception ignored) {}
                if (unique.size() >= target) break;
            }
        }

        // 2) Fallback σε TMDb discover αν δεν έχουμε αρκετά
        if (unique.size() < target) {
            String genreCsv = mapTagToGenresCsv(tag);

            Random rnd = new Random();
            int page = 1 + rnd.nextInt(3);

            try {
                MovieSearchResponse r = tmdbService.discoverMovies(
                        page,
                        "popularity.desc",
                        genreCsv,
                        null,
                        null
                );

                if (r != null && r.getResults() != null) {
                    for (MovieResultDto m : r.getResults()) {
                        if (m == null) continue;
                        if (m.getId() <= 0) continue;
                        if (m.getTitle() == null || m.getTitle().isBlank()) continue;
                        if (m.getPoster_path() == null || m.getPoster_path().isBlank()) continue;

                        unique.putIfAbsent(m.getId(), m);
                        if (unique.size() >= target) break;
                    }
                }
            } catch (Exception ignored) {}
        }

        List<MovieResultDto> results = unique.values().stream().limit(target).toList();

        return new RecommendByMoodResponse(label, score, tag, results);
    }

    private String mapLabelToTag(String label) {
        if (label == null) return "neutral";
        return switch (label.toLowerCase()) {
            case "positive" -> "uplifting";
            case "negative" -> "dark";
            case "neutral" -> "neutral";
            default -> "neutral";
        };
    }

    private String mapTagToGenresCsv(String tag) {
        // TMDb genre ids:
        // 35 Comedy, 10751 Family, 12 Adventure, 16 Animation, 14 Fantasy
        // 27 Horror, 53 Thriller, 80 Crime, 9648 Mystery
        // 18 Drama, 878 Sci-Fi, 10749 Romance
        if (tag == null) return "18";

        return switch (tag.toLowerCase()) {
            case "uplifting" -> "35,10751,12,16,14";
            case "dark" -> "27,53,80,9648";
            case "neutral" -> "18,878,10749";
            default -> "18";
        };
    }
}
