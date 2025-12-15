package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FaceMatchResponse {

    private String actor;       // Actor full name
    private double similarity;  // Cosine similarity [0..1]
    private Long actorId;       // TMDB actor id
    private String actorImage;  // TMDB profile image URL
}
