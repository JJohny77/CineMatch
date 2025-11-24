package com.cinematch.backend.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeaderboardEntry {
    private String email;
    private Integer score;
}
