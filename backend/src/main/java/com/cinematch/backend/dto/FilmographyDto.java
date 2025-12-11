package com.cinematch.backend.dto;

import lombok.Data;

@Data
public class FilmographyDto {

    private Long movieId;
    private String title;
    private String character;
    private Integer releaseYear;
}
