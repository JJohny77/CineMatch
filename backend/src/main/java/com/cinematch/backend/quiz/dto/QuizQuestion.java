package com.cinematch.backend.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class QuizQuestion {
    private String question;
    private List<String> options;
}
