package com.cinematch.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceScoreDto {
    private Long id;
    private double score;
}
