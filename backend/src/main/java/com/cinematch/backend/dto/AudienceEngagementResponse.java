package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AudienceEngagementResponse {
    private Long movieId;
    private int engagement;
}
