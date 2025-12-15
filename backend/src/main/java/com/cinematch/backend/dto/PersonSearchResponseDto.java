package com.cinematch.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class PersonSearchResponseDto {

    private Integer page;
    private Integer totalPages;
    private Long totalResults;
    private List<PersonSearchResultDto> results;
}
