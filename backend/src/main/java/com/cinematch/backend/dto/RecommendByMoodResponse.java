package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecommendByMoodResponse {
    private String sentiment;          // positive / negative / neutral / loading
    private double score;              // confidence (όπως στο SentimentResponse)
    private String tag;                // uplifting / dark / neutral
    private List<MovieResultDto> results;
}
