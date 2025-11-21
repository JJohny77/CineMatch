package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovieVideoDto {
    private String name;
    private String key;     // YouTube video key
    private String site;    // YouTube
    private String type;    // Trailer, Teaser etc.
}
