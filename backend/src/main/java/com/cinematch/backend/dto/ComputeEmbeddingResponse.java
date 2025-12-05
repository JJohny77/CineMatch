package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ComputeEmbeddingResponse {

    private Long actorId;
    private String name;
    private String imageUrl;
    private int embeddingLength;
}
