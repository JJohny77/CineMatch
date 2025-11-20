package com.cinematch.backend.quiz.dto;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private String question;
    private String selectedOption;
}
