package com.cinematch.backend.dto;

import lombok.Data;

@Data
public class ComputeEmbeddingRequest {

    private Long actorId;      // TMDB person id
    private String name;       // Actor name
    private String imageUrl;   // Full URL προς το profile image
}
