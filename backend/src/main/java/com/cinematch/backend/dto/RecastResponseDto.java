package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecastResponseDto {
    private String actor;
    private double similarity;
    private long actorId;
}
