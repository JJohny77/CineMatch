package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDetailsDto {
    private String title;
    private String overview;
    private String poster_path;
    private String release_date;
    private Integer runtime;
    private Double popularity;
    private List<String> genres;
}
