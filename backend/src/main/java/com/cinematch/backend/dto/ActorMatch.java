package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActorMatch {

    private Long actorId;
    private String name;
    private String imageUrl;
    private double similarity;
}
