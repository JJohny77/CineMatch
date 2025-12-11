package com.cinematch.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ActorDetailsDto {

    private Long id;
    private String name;
    private String profilePath;

    private String biography;
    private String birthday;
    private String placeOfBirth;

    // 5–10 γνωστές ταινίες
    private List<KnownForDto> knownFor;

    // πλήρης φιλμογραφία (ταινία + ρόλος + έτος)
    private List<FilmographyDto> filmography;
}
