package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendingPersonDto {
    private Long id;
    private String name;
    private String profilePath;
    private String department;
    private Double popularity;
}
