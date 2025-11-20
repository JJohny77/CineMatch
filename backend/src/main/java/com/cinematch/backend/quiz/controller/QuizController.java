package com.cinematch.backend.quiz.controller;

import com.cinematch.backend.quiz.dto.QuizResponse;
import com.cinematch.backend.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/start")
    public ResponseEntity<QuizResponse> startQuiz() {

        QuizResponse response = quizService.startQuiz();
        return ResponseEntity.ok(response);
    }
}
