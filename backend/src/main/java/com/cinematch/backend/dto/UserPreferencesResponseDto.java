package com.cinematch.backend.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesResponseDto {
    private List<PreferenceScoreDto> topGenres;     // id = genreId
    private List<PreferenceScoreDto> topActors;     // id = personId
    private List<PreferenceScoreDto> topDirectors;  // id = personId
    private Instant lastUpdated;
}
