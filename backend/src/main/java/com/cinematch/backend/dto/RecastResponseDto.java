package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecastResponseDto {

    private String actor;      // Όνομα ηθοποιού
    private double similarity; // Cosine similarity (0..1)
    private Long actorId;      // ID από τη βάση
    private String actorImage; // URL εικόνας ηθοποιού
}
