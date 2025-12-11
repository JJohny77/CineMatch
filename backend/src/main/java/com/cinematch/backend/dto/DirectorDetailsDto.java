package com.cinematch.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class DirectorDetailsDto {

    private Long id;
    private String name;
    private String profilePath;

    private String biography;
    private String birthday;
    private String placeOfBirth;

    // Λίστα ταινιών που έχει σκηνοθετήσει (μέχρι ~20)
    private List<KnownForDto> directedMovies;
}
