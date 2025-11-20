package com.cinematch.backend.quiz.controller;

import com.cinematch.backend.quiz.dto.QuizResponse;
import com.cinematch.backend.quiz.dto.SubmitAnswerRequest;
import com.cinematch.backend.quiz.dto.SubmitAnswerResponse;
import com.cinematch.backend.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // ================================
    // US21 — Start Quiz
    // ================================
    @PostMapping("/start")
    public ResponseEntity<QuizResponse> startQuiz() {
        QuizResponse response = quizService.startQuiz();
        return ResponseEntity.ok(response);
    }

    // ================================
    // US22 — Submit Answer
    // ================================
    @PostMapping("/answer")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @RequestBody SubmitAnswerRequest request
    ) {

        boolean isCorrect = quizService.checkAnswer(
                request.getQuestion(),
                request.getSelectedOption()
        );

        return ResponseEntity.ok(new SubmitAnswerResponse(isCorrect));
    }
}
