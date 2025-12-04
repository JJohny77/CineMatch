package com.cinematch.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonDto {

    private long id;

    private String name;

    @JsonProperty("original_name")
    private String originalName;

    @JsonProperty("profile_path")
    private String profilePath;

    @JsonProperty("known_for_department")
    private String knownForDepartment;

    private double popularity;

    // Αν θες αργότερα, μπορούμε να το μοντελοποιήσουμε σωστά.
    // Προς το παρόν το αφήνουμε "απλό" για να μην σπάει το mapping.
    @JsonProperty("known_for")
    private List<Object> knownFor;
}
