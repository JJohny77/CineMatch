package com.cinematch.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class PersonSearchResultDto {

    private Long id;
    private String name;
    private String profilePath;
    private String knownForDepartment;
    private List<String> knownFor;
    private Double popularity;
}
