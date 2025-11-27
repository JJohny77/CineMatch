package com.cinematch.backend.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmitAnswerResponse {

    @JsonProperty("isCorrect")
    private boolean isCorrect;

    private String selectedAnswer;
    private String correctAnswer;
    private boolean showResult;
}