package com.cinematch.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class MovieSearchResponse {
    private int page;
    private int totalPages;
    private int totalResults;
    private List<MovieResultDto> results;
}
