package com.cinematch.backend.quiz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.ArrayList;
import com.cinematch.backend.quiz.dto.*;
import com.cinematch.backend.quiz.service.QuizService;
import com.cinematch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;
    private final UserRepository userRepository;

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

        // ΠΑΝΤΑ δίνουμε την πραγματική σωστή απάντηση
        String correct = quizService.getCorrectAnswer(request.getQuestion());

        return ResponseEntity.ok(
                new SubmitAnswerResponse(
                        isCorrect,
                        request.getSelectedOption(),
                        correct,
                        true
                )
        );
    }


    @PostMapping("/finish")
    public ResponseEntity<FinishQuizResponse> finishQuiz(
            @RequestBody FinishQuizRequest request
    ) {
        quizService.saveQuizScore(request.getScore(), userRepository);
        return ResponseEntity.ok(new FinishQuizResponse(true));
    }

    // ================================
    // US24 — Leaderboard
    // ================================
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        return ResponseEntity.ok(quizService.getLeaderboard(userRepository));
    }
}
