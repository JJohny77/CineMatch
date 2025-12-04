package com.cinematch.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class GenreListResponse {
    private List<GenreDto> genres;
}
