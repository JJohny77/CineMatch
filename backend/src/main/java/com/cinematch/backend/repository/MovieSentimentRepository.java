package com.cinematch.backend.repository;

import com.cinematch.backend.model.ai.MovieSentiment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieSentimentRepository extends JpaRepository<MovieSentiment, Long> {

    List<MovieSentiment> findByMovieId(Long movieId);

    boolean existsByMovieIdAndTagAndSource(Long movieId, String tag, String source);

    List<MovieSentiment> findTop200ByTagOrderByScoreDesc(String tag);
}