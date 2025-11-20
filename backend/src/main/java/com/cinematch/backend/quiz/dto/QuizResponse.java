package com.cinematch.backend.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class QuizResponse {
    private List<QuizQuestion> questions;
}
