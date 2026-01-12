package com.cinematch.backend.model.ai;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movie_sentiment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieSentiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TMDb movie id
    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    // uplifting, dark, emotional, neutral, etc
    @Column(nullable = false)
    private String tag;

    // confidence score from model
    @Column(nullable = false)
    private double score;

    // overview / review
    @Column(nullable = false)
    private String source;
}