package com.cinematch.backend.quiz.service;

import java.util.List;

public record FullQuestion(
        String question,
        String correctAnswer,
        List<String> options
) {}
