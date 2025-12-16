package com.cinematch.backend.service.ai;

import com.cinematch.backend.dto.SentimentResponse;
import com.cinematch.backend.model.ai.MovieSentiment;
import com.cinematch.backend.repository.MovieSentimentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieSentimentService {

    private final SentimentService sentimentService;
    private final MovieSentimentRepository movieSentimentRepository;

    public MovieSentiment analyzeAndStore(
            Long movieId,
            String text,
            String source
    ) {
        SentimentResponse response = sentimentService.analyze(text);

        String tag = mapLabelToTag(response.getSentiment());

        // αποφυγή διπλότυπων
        if (movieSentimentRepository.existsByMovieIdAndTagAndSource(
                movieId, tag, source
        )) {
            return null;
        }

        MovieSentiment sentiment = MovieSentiment.builder()
                .movieId(movieId)
                .tag(tag)
                .score(response.getScore())
                .source(source)
                .build();

        return movieSentimentRepository.save(sentiment);
    }

    private String mapLabelToTag(String label) {
        return switch (label) {
            case "positive" -> "uplifting";
            case "negative" -> "dark";
            case "neutral" -> "neutral";
            default -> "complex";
        };
    }
}